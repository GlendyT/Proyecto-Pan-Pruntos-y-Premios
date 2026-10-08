package com.panpuntospremios.api.servicio;

import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${MAIL_FROM:${spring.mail.username:}}")
    private String remitente;

    @Value("${app.email.provider:smtp}")
    private String proveedor;

    @Value("${emailjs.service-id:}")
    private String emailJsServiceId;

    @Value("${emailjs.template-id:}")
    private String emailJsTemplateId;

    @Value("${emailjs.public-key:}")
    private String emailJsPublicKey;

    @Value("${emailjs.private-key:}")
    private String emailJsPrivateKey;

    private final RestClient restClient = RestClient.create();

    public void enviarEnlaceAcceso(String destinatario, String nombreCliente, String enlace) {
        log.info("Procesando solicitud de envío de enlace de acceso");

        if ("emailjs".equalsIgnoreCase(proveedor)) {
            enviarViaEmailJs(destinatario, nombreCliente, enlace);
            return;
        }

        if (mailSender != null && remitente != null && !remitente.isBlank()) {
            try {
                SimpleMailMessage mensaje = new SimpleMailMessage();
                mensaje.setFrom(remitente);
                mensaje.setTo(destinatario);
                mensaje.setSubject("Tu enlace de acceso a Pan, Puntos y Premios");
                mensaje.setText(String.format(
                    "Hola %s,\n\n" +
                    "Haz clic en el siguiente enlace para consultar tu saldo e historial de puntos:\n\n" +
                    "%s\n\n" +
                    "Este enlace expira en 15 minutos y solo puede utilizarse una vez.\n" +
                    "Si no solicitaste este acceso, puedes ignorar este mensaje.\n\n" +
                    "Pan, Puntos y Premios",
                    nombreCliente != null ? nombreCliente : "Cliente",
                    enlace
                ));
                mailSender.send(mensaje);
                log.info("Correo de acceso enviado exitosamente");
                return;
            } catch (Exception e) {
                // MailSendException guarda los fallos por mensaje aparte de getCause(). Registrar
                // únicamente tipos de excepción, nunca mensajes con destinatario u otros secretos.
                log.warn("No se pudo enviar el correo de acceso vía SMTP (tipo={}, causas={})",
                    e.getClass().getSimpleName(), diagnosticoSeguro(e));
            }
        } else {
            log.warn("Servidor de correo no configurado. El enlace de acceso no fue despachado.");
        }
    }

    private void enviarViaEmailJs(String destinatario, String nombreCliente, String enlace) {
        if (emailJsServiceId.isBlank() || emailJsTemplateId.isBlank()
                || emailJsPublicKey.isBlank() || emailJsPrivateKey.isBlank()) {
            log.warn("EmailJS seleccionado pero faltan variables de configuración");
            return;
        }

        try {
            var templateParams = new java.util.HashMap<String, String>();
            templateParams.put("to_email", destinatario);
            templateParams.put("to_name", nombreCliente != null ? nombreCliente : "Cliente");
            templateParams.put("access_link", enlace);
            templateParams.put("expiration_minutes", "15");

            var payload = new java.util.HashMap<String, Object>();
            payload.put("service_id", emailJsServiceId);
            payload.put("template_id", emailJsTemplateId);
            payload.put("user_id", emailJsPublicKey);
            payload.put("accessToken", emailJsPrivateKey);
            payload.put("template_params", templateParams);

            restClient.post()
                .uri("https://api.emailjs.com/api/v1.0/email/send")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
            log.info("Correo de acceso enviado exitosamente vía EmailJS");
        } catch (Exception e) {
            log.warn("No se pudo enviar el correo de acceso vía EmailJS (tipo={})",
                e.getClass().getSimpleName());
        }
    }

    private String diagnosticoSeguro(Exception error) {
        Set<Throwable> visitados = Collections.newSetFromMap(new IdentityHashMap<>());
        ArrayDeque<Throwable> pendientes = new ArrayDeque<>();
        java.util.LinkedHashSet<String> tipos = new java.util.LinkedHashSet<>();
        pendientes.add(error);

        if (error instanceof MailSendException mailSendException) {
            for (Exception excepcionMensaje : mailSendException.getMessageExceptions()) {
                if (excepcionMensaje != null) {
                    pendientes.add(excepcionMensaje);
                }
            }
        }

        while (!pendientes.isEmpty()) {
            Throwable actual = pendientes.removeFirst();
            if (!visitados.add(actual)) {
                continue;
            }
            tipos.add(actual.getClass().getSimpleName());
            if (actual.getCause() != null) {
                pendientes.addLast(actual.getCause());
            }
            if (actual instanceof MessagingException messagingException
                    && messagingException.getNextException() != null) {
                pendientes.addLast(messagingException.getNextException());
            }
        }

        return String.join("->", tipos);
    }
}
