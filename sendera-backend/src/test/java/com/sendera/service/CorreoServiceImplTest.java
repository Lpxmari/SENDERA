package com.sendera.service;

import com.sendera.service.impl.CorreoServiceImpl;
import com.sun.net.httpserver.HttpServer;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** El correo de recuperación nunca debe romper el flujo: ante cualquier fallo solo se registra. */
@ExtendWith(MockitoExtension.class)
class CorreoServiceImplTest {

    @Mock private ObjectProvider<JavaMailSender> proveedor;
    @Mock private JavaMailSender sender;

    private CorreoServiceImpl service;
    private HttpServer brevo;
    private final AtomicReference<String> cuerpoRecibido = new AtomicReference<>();
    private final AtomicReference<String> claveRecibida = new AtomicReference<>();

    @BeforeEach
    void preparar() {
        service = new CorreoServiceImpl(proveedor);
        ReflectionTestUtils.setField(service, "brevoApiKey", "");
        ReflectionTestUtils.setField(service, "brevoUrl", "http://localhost:1");
        ReflectionTestUtils.setField(service, "remitente", "no-reply@sendera.com");
        ReflectionTestUtils.setField(service, "nombreRemitente", "Sendera");
        ReflectionTestUtils.setField(service, "minutosExpiracion", 15L);
    }

    @AfterEach
    void cerrar() {
        if (brevo != null) {
            brevo.stop(0);
        }
    }

    /** Levanta un Brevo falso que responde con el código indicado. */
    private void iniciarBrevo(int codigo) throws IOException {
        brevo = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        brevo.createContext("/smtp/email", intercambio -> {
            cuerpoRecibido.set(new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            claveRecibida.set(intercambio.getRequestHeaders().getFirst("api-key"));
            byte[] respuesta = "{\"messageId\":\"x\"}".getBytes(StandardCharsets.UTF_8);
            intercambio.sendResponseHeaders(codigo, respuesta.length);
            intercambio.getResponseBody().write(respuesta);
            intercambio.close();
        });
        brevo.start();
        ReflectionTestUtils.setField(service, "brevoApiKey", "clave-de-prueba");
        ReflectionTestUtils.setField(service, "brevoUrl", "http://localhost:" + brevo.getAddress().getPort());
    }

    @Test
    void conBrevoEnviaElCodigoAlDestinatarioConLaClaveDeApi() throws IOException {
        iniciarBrevo(201);

        service.enviarCodigoRecuperacion("maria@correo.com", "María", "123456");

        assertEquals("clave-de-prueba", claveRecibida.get());
        assertTrue(cuerpoRecibido.get().contains("maria@correo.com"));
        assertTrue(cuerpoRecibido.get().contains("123456"));
        verifyNoInteractions(proveedor);
    }

    @Test
    void elNombreSeEscapaEnElHtmlDelCorreo() throws IOException {
        iniciarBrevo(201);

        service.enviarCodigoRecuperacion("x@correo.com", "<script>alert(1)</script>", "123456");

        // El texto plano conserva el nombre tal cual; el HTML nunca debe llevar la etiqueta sin escapar.
        assertFalse(cuerpoRecibido.get().contains("<p>Hola <script>"));
        assertTrue(cuerpoRecibido.get().contains("<p>Hola &lt;script&gt;alert(1)&lt;/script&gt;,</p>"));
    }

    @Test
    void siBrevoRechazaElEnvioNoLanzaExcepcionNiUsaSmtp() throws IOException {
        iniciarBrevo(500);

        assertDoesNotThrow(() -> service.enviarCodigoRecuperacion("maria@correo.com", "María", "123456"));
        verifyNoInteractions(proveedor);
    }

    @Test
    void siBrevoNoEsAlcanzableNoLanzaExcepcion() {
        ReflectionTestUtils.setField(service, "brevoApiKey", "clave-de-prueba");

        assertDoesNotThrow(() -> service.enviarCodigoRecuperacion("maria@correo.com", "María", "123456"));
    }

    @Test
    void sinBrevoUsaSmtpYEnviaElMensaje() {
        MimeMessage mensaje = new MimeMessage((Session) null);
        when(proveedor.getIfAvailable()).thenReturn(sender);
        when(sender.createMimeMessage()).thenReturn(mensaje);

        service.enviarCodigoRecuperacion("maria@correo.com", "María", "123456");

        verify(sender).send(mensaje);
    }

    @Test
    void siSmtpFallaNoLanzaExcepcion() {
        when(proveedor.getIfAvailable()).thenReturn(sender);
        when(sender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        doThrow(new MailSendException("sin conexión")).when(sender).send(any(MimeMessage.class));

        assertDoesNotThrow(() -> service.enviarCodigoRecuperacion("maria@correo.com", "María", "123456"));
    }

    @Test
    void sinBrevoNiSmtpSoloRegistraUnAviso() {
        when(proveedor.getIfAvailable()).thenReturn(null);

        assertDoesNotThrow(() -> service.enviarCodigoRecuperacion("maria@correo.com", "María", "123456"));
    }
}
