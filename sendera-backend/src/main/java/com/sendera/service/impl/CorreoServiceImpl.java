package com.sendera.service.impl;

import com.sendera.service.CorreoService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.HtmlUtils;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CorreoServiceImpl implements CorreoService {

    private final ObjectProvider<JavaMailSender> mailSender;

    @Value("${app.mail.brevo-api-key:}")
    private String brevoApiKey;

    @Value("${app.mail.brevo-url:https://api.brevo.com/v3}")
    private String brevoUrl;

    @Value("${app.mail.remitente:no-reply@sendera.com}")
    private String remitente;

    @Value("${app.mail.nombre-remitente:Sendera}")
    private String nombreRemitente;

    @Value("${app.recuperacion.expiracion-minutos}")
    private long minutosExpiracion;

    @Async
    @Override
    public void enviarCodigoRecuperacion(String correo, String nombre, String codigo) {
        if (!brevoApiKey.isBlank()) {
            try {
                SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
                requestFactory.setConnectTimeout(10_000);
                requestFactory.setReadTimeout(10_000);

                RestClient restClient = RestClient.builder()
                        .requestFactory(requestFactory)
                        .baseUrl(brevoUrl)
                        .defaultHeaders(headers -> {
                            headers.set("api-key", brevoApiKey);
                            headers.setContentType(MediaType.APPLICATION_JSON);
                            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
                        })
                        .build();

                Map<String, Object> payload = Map.of(
                        "sender", Map.of("name", nombreRemitente, "email", remitente),
                        "to", List.of(Map.of("email", correo, "name", nombre)),
                        "subject", "Tu código para restablecer la contraseña de Sendera",
                        "htmlContent", htmlCorreo(nombre, codigo),
                        "textContent", textoPlano(nombre, codigo)
                );

                restClient.post()
                        .uri("/smtp/email")
                        .body(payload)
                        .retrieve()
                        .toBodilessEntity();
                return;
            } catch (RestClientResponseException e) {
                log.error("No se pudo enviar el correo de recuperación por Brevo. HTTP {}. Respuesta: {}",
                        e.getStatusCode().value(), e.getResponseBodyAsString());
                return;
            } catch (RestClientException e) {
                log.error("No se pudo enviar el correo de recuperación por Brevo: {}", e.getMessage());
                return;
            }
        }

        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender != null) {
            try {
                MimeMessage mensaje = sender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
                helper.setFrom(remitente, nombreRemitente);
                helper.setTo(correo);
                helper.setSubject("Tu código para restablecer la contraseña de Sendera");
                helper.setText(textoPlano(nombre, codigo), htmlCorreo(nombre, codigo));
                sender.send(mensaje);
                return;
            } catch (MessagingException | MailException | UnsupportedEncodingException e) {
                log.error("No se pudo enviar el correo de recuperación por SMTP: {}", e.getMessage());
            }
        }

        log.warn("No se pudo enviar el correo de recuperación. Revisa la configuración de SMTP o Brevo.");
    }

    private String textoPlano(String nombre, String codigo) {
        return "Hola " + nombre + ",\n\n"
                + "Tu código para restablecer la contraseña es: " + codigo + "\n"
                + "El código vence en " + minutosExpiracion + " minutos.\n\n"
                + "Si no fuiste tú, ignora este mensaje: tu contraseña no cambiará.\n\nEquipo Sendera";
    }

    private String htmlCorreo(String nombre, String codigo) {
        String nombreSeguro = HtmlUtils.htmlEscape(nombre);
        return """
                <div style="font-family:Arial,sans-serif;max-width:480px;margin:auto;padding:24px;background:#fcf8e3;border-radius:8px">
                  <h2 style="color:#2d6a4f;margin-top:0">Recupera tu contraseña</h2>
                  <p>Hola %s,</p>
                  <p>Tu código para restablecer la contraseña es:</p>
                  <div style="text-align:center;margin:24px 0;padding:18px 12px;background:#fff;border-radius:8px;border:1px solid #d9d5c1;">
                    <span style="display:inline-block;font-size:32px;font-weight:700;letter-spacing:8px;color:#2d6a4f">%s</span>
                  </div>
                  <p>El código vence en %d minutos.</p>
                  <p style="font-size:13px;color:#6b6b6b">Si no fuiste tú, ignora este mensaje: tu contraseña no cambiará.</p>
                </div>
                """.formatted(nombreSeguro, codigo, minutosExpiracion);
    }
}
