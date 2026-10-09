package com.sendera.service.impl;

import com.sendera.service.CorreoService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class CorreoServiceImpl implements CorreoService {

    private final ObjectProvider<JavaMailSender> mailSender;

    @Value("${app.mail.remitente}")
    private String remitente;

    @Value("${app.recuperacion.expiracion-minutos}")
    private long minutosExpiracion;

    /** Se ejecuta en segundo plano para que el tiempo de respuesta no delate si el correo existe. */
    @Async
    @Override
    public void enviarRecuperacionContrasena(String correo, String nombre, String enlace) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            // Solo desarrollo: sin SMTP configurado (SPRING_MAIL_HOST) el enlace se imprime en consola.
            log.warn("SMTP no configurado. Enlace de recuperación para {}: {}", correo, enlace);
            return;
        }
        try {
            MimeMessage mensaje = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            helper.setFrom(remitente);
            helper.setTo(correo);
            helper.setSubject("Recupera tu contraseña en Sendera");
            helper.setText(textoPlano(nombre, enlace), htmlCorreo(nombre, enlace));
            sender.send(mensaje);
        } catch (MessagingException | MailException e) {
            log.error("No se pudo enviar el correo de recuperación a {}", correo, e);
        }
    }

    private String textoPlano(String nombre, String enlace) {
        return "Hola " + nombre + ",\n\n"
                + "Recibimos una solicitud para cambiar tu contraseña. Abre este enlace (válido por "
                + minutosExpiracion + " minutos):\n" + enlace + "\n\n"
                + "Si no fuiste tú, ignora este mensaje.\n\nEquipo Sendera";
    }

    private String htmlCorreo(String nombre, String enlace) {
        String nombreSeguro = HtmlUtils.htmlEscape(nombre);
        return """
                <div style="font-family:Arial,sans-serif;max-width:480px;margin:auto;padding:24px;background:#fcf8e3;border-radius:8px">
                  <h2 style="color:#2d6a4f;margin-top:0">Recupera tu contraseña</h2>
                  <p>Hola %s,</p>
                  <p>Recibimos una solicitud para cambiar la contraseña de tu cuenta en Sendera.
                     El enlace es válido por %d minutos.</p>
                  <p style="text-align:center;margin:28px 0">
                    <a href="%s" style="background:#2d6a4f;color:#ffffff;padding:12px 24px;border-radius:6px;text-decoration:none;font-weight:bold">
                      Cambiar contraseña</a>
                  </p>
                  <p style="font-size:13px;color:#6b6b6b">Si no fuiste tú, ignora este mensaje: tu contraseña no cambiará.</p>
                </div>
                """.formatted(nombreSeguro, minutosExpiracion, enlace);
    }
}
