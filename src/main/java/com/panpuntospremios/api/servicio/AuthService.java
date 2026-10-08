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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collections;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    public static final String MENSAJE_GENERICO_SOLICITUD =
        "Si la dirección de correo corresponde a una cuenta activa, hemos enviado un enlace de acceso.";

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

        if (optCliente.isEmpty()) {
            log.info("Solicitud de acceso sin envío: no se encontró una cuenta para el correo recibido");
            return new AuthMensajeDTO(MENSAJE_GENERICO_SOLICITUD);
        }

        ClienteAuth cliente = optCliente.get();
        // El primer enlace enviado al correo registrado verifica cuentas aún no verificadas.
        if (!cliente.isActivo()) {
            log.info("Solicitud de acceso sin envío: la cuenta encontrada está inactiva");
            return new AuthMensajeDTO(MENSAJE_GENERICO_SOLICITUD);
        }

        // Rate Limiting: Cooldown de 60s, máximo 3 solicitudes en 10 minutos
        boolean superaLimite = authDAO.superaLimiteSolicitudes(
            cliente.getIdCliente(), 60, 10, 3
        );
        if (superaLimite) {
            log.info("Solicitud de acceso sin envío: cooldown o cuota por cliente activos");
            return new AuthMensajeDTO(MENSAJE_GENERICO_SOLICITUD);
        }

        String rawToken = generarTokenAleatorio();
        String tokenHash = calcularSha256(rawToken);
        authDAO.registrarToken(cliente.getIdCliente(), tokenHash, expiracionMinutos, ip, userAgent);

        // Fragmento URL seguro para que no se filtre en logs de servidores o proxies
        String enlace = portalBaseUrl + "/auth/callback#token=" + rawToken;
        emailService.enviarEnlaceAcceso(cliente.getEmail(), cliente.getNombre(), enlace);

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
