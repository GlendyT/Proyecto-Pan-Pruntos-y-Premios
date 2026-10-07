package com.panpuntospremios.api.servicio;

import com.panpuntospremios.api.dao.AuthDAO;
import com.panpuntospremios.api.dto.AuthMensajeDTO;
import com.panpuntospremios.api.excepcion.LimitePeticionesException;
import com.panpuntospremios.api.modelo.ClienteAuth;
import com.panpuntospremios.api.modelo.ClientePrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.Collections;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class AuthService {

    public static final String MENSAJE_GENERICO_SOLICITUD =
        "Si la dirección de correo corresponde a una cuenta activa y verificada, hemos enviado un enlace de acceso.";

    private final AuthDAO authDAO;
    private final EmailService emailService;
    private final IpRateLimiter ipRateLimiter;
    private final SecurityContextRepository securityContextRepository;

    @Value("${app.portal.base-url:http://localhost:3000}")
    private String portalBaseUrl;

    @Value("${app.auth.token-expiration-minutes:15}")
    private int expiracionMinutos;

    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(AuthDAO authDAO, EmailService emailService, IpRateLimiter ipRateLimiter) {
        this.authDAO = authDAO;
        this.emailService = emailService;
        this.ipRateLimiter = ipRateLimiter;
        this.securityContextRepository = new HttpSessionSecurityContextRepository();
    }

    public AuthMensajeDTO solicitarAcceso(String email, String ip, String userAgent) {
        if (!ipRateLimiter.permiteSolicitud(ip)) {
            throw new LimitePeticionesException("Ha superado el límite de solicitudes permitidas desde su dirección IP. Intente más tarde.");
        }

        if (email == null || email.isBlank()) {
            return new AuthMensajeDTO(MENSAJE_GENERICO_SOLICITUD);
        }

        Optional<ClienteAuth> optCliente = authDAO.buscarClientePorEmail(email.trim());

        if (optCliente.isPresent()) {
            ClienteAuth cliente = optCliente.get();
            // Regla de seguridad: Solo clientes activos y con email_verificado = 'S'
            if (cliente.isActivo() && cliente.isEmailVerificado()) {
                // Rate Limiting: Cooldown de 60s, máximo 3 solicitudes en 10 minutos
                boolean superaLimite = authDAO.superaLimiteSolicitudes(
                    cliente.getIdCliente(), 60, 10, 3
                );

                if (!superaLimite) {
                    String rawToken = generarTokenAleatorio();
                    String tokenHash = calcularSha256(rawToken);
                    Timestamp expiracion = new Timestamp(
                        System.currentTimeMillis() + ((long) expiracionMinutos * 60 * 1000)
                    );

                    authDAO.registrarToken(cliente.getIdCliente(), tokenHash, expiracion, ip, userAgent);

                    // Fragmento URL seguro para que no se filtre en logs de servidores o proxies
                    String enlace = portalBaseUrl + "/auth/callback#token=" + rawToken;
                    emailService.enviarEnlaceAcceso(cliente.getEmail(), cliente.getNombre(), enlace);
                }
            }
        }

        // Siempre devuelve el mismo mensaje para mitigar ataques de enumeración
        return new AuthMensajeDTO(MENSAJE_GENERICO_SOLICITUD);
    }

    public AuthMensajeDTO consumirToken(String rawToken, HttpServletRequest request, HttpServletResponse response) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BadCredentialsException("El enlace de acceso es inválido o ha expirado.");
        }

        String tokenHash = calcularSha256(rawToken.trim());
        Optional<Integer> optIdCliente = authDAO.consumirToken(tokenHash);

        if (optIdCliente.isEmpty()) {
            throw new BadCredentialsException("El enlace de acceso es inválido o ha expirado.");
        }

        int idCliente = optIdCliente.get();
        ClienteAuth cliente = authDAO.buscarClientePorId(idCliente)
            .orElseThrow(() -> new BadCredentialsException("Cliente no encontrado para el token consumido."));

        if (!cliente.isActivo()) {
            throw new BadCredentialsException("La cuenta del cliente no se encuentra activa.");
        }

        // Crear sesión segura en Spring Security
        ClientePrincipal principal = new ClientePrincipal(
            cliente.getIdCliente(),
            cliente.getIdentificador(),
            cliente.getNombre(),
            cliente.getEmail()
        );

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        return new AuthMensajeDTO(
            "Sesión iniciada correctamente",
            cliente.getIdCliente(),
            cliente.getIdentificador(),
            cliente.getNombre(),
            cliente.getEmail()
        );
    }

    public AuthMensajeDTO cerrarSesion(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return new AuthMensajeDTO("Sesión cerrada correctamente");
    }

    private String generarTokenAleatorio() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String calcularSha256(String texto) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algoritmo criptográfico SHA-256 no disponible", e);
        }
    }
}
