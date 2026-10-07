package com.panpuntospremios.api.servicio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String remitente;

    public void enviarEnlaceAcceso(String destinatario, String nombreCliente, String enlace) {
        log.info("Procesando solicitud de enlace de acceso para {}", destinatario);

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
                log.info("Correo de acceso enviado exitosamente a {}", destinatario);
                return;
            } catch (Exception e) {
                log.warn("No se pudo enviar el correo a {} vía SMTP: {}", destinatario, e.getMessage());
            }
        } else {
            log.warn("Servidor de correo no configurado. El correo de acceso no fue despachado para {}.", destinatario);
            log.debug("Enlace de acceso para depuración local: {}", enlace);
        }
    }
}
