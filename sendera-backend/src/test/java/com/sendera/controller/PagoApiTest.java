package com.sendera.controller;

import com.sendera.config.SecurityConfig;
import com.sendera.dto.PreferenciaPagoDto;
import com.sendera.dto.ReservaDto;
import com.sendera.exception.GlobalExceptionHandler;
import com.sendera.exception.PagoNoDisponibleException;
import com.sendera.exception.PasarelaPagoException;
import com.sendera.integration.ValidadorFirmaMercadoPago;
import com.sendera.model.Usuario;
import com.sendera.security.CuentaActivaVerificador;
import com.sendera.security.JwtService;
import com.sendera.security.UsuarioActual;
import com.sendera.service.PagoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RF-014: endpoints de pago (con JWT) y webhook público de Mercado Pago autenticado por firma. */
@WebMvcTest(PagoController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, JwtService.class, ValidadorFirmaMercadoPago.class})
@TestPropertySource(properties = {
        "app.jwt.secret=clave-de-pruebas-con-mas-de-treinta-y-dos-caracteres",
        "app.jwt.expiracion-horas=1",
        "app.cors.allowed-origins=http://localhost:5173",
        "app.pagos.mercadopago.webhook-secret=" + PagoApiTest.SECRETO
})
class PagoApiTest {

    static final String SECRETO = "secreto-webhook-de-pruebas";

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @MockBean PagoService pagoService;
    @MockBean CuentaActivaVerificador cuentaActiva;

    @BeforeEach
    void cuentaActiva() {
        when(cuentaActiva.estaActiva(7L)).thenReturn(true);
    }

    private String token() {
        return "Bearer " + jwtService.generar(Usuario.builder().id(7L).correo("a@b.com").rol("ROLE_CLIENTE").build());
    }

    private static String firma(String manifiesto) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRETO.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(manifiesto.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void iniciarPagoDevuelveLaUrlDeMercadoPago() throws Exception {
        when(pagoService.iniciarPago(15L, new UsuarioActual(7L, false)))
                .thenReturn(new PreferenciaPagoDto("pref-1", "https://www.mercadopago.com.co/checkout/v1/redirect?pref_id=pref-1"));

        mockMvc.perform(post("/reservas/15/pago").header("Authorization", token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preferenciaId").value("pref-1"))
                .andExpect(jsonPath("$.urlPago").value("https://www.mercadopago.com.co/checkout/v1/redirect?pref_id=pref-1"));
    }

    @Test
    void pagarYVerificarExigenSesion() throws Exception {
        mockMvc.perform(post("/reservas/15/pago")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/reservas/15/pago/verificar")).andExpect(status().isUnauthorized());
        verify(pagoService, never()).iniciarPago(any(), any());
    }

    @Test
    void verificarDevuelveLaReservaConSuEstadoDePago() throws Exception {
        when(pagoService.verificarPago(15L, new UsuarioActual(7L, false))).thenReturn(
                ReservaDto.builder().id(15L).estadoReserva("CONFIRMADA").estadoPago("APROBADO").build());

        mockMvc.perform(post("/reservas/15/pago/verificar").header("Authorization", token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoReserva").value("CONFIRMADA"))
                .andExpect(jsonPath("$.estadoPago").value("APROBADO"));
    }

    @Test
    void sinCredencialesResponde503YSiMercadoPagoFalla502() throws Exception {
        when(pagoService.iniciarPago(15L, new UsuarioActual(7L, false))).thenThrow(new PagoNoDisponibleException());
        mockMvc.perform(post("/reservas/15/pago").header("Authorization", token()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("El pago en línea no está disponible por ahora. Intenta más tarde."));

        when(pagoService.iniciarPago(16L, new UsuarioActual(7L, false))).thenThrow(new PasarelaPagoException());
        mockMvc.perform(post("/reservas/16/pago").header("Authorization", token()))
                .andExpect(status().isBadGateway());
    }

    @Test
    void webhookFirmadoEsPublicoYSeProcesa() throws Exception {
        String v1 = firma("id:987;request-id:req-1;ts:1700000000;");

        mockMvc.perform(post("/pagos/webhook").param("data.id", "987").param("type", "payment")
                        .header("x-signature", "ts=1700000000,v1=" + v1).header("x-request-id", "req-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"payment.updated\",\"type\":\"payment\",\"data\":{\"id\":\"987\"}}"))
                .andExpect(status().isOk());

        verify(pagoService).procesarNotificacion("payment", "987");
    }

    @Test
    void webhookConFirmaInvalidaOAusenteResponde401() throws Exception {
        mockMvc.perform(post("/pagos/webhook").param("data.id", "987").param("type", "payment")
                        .header("x-signature", "ts=1,v1=" + firma("id:111;request-id:req-1;ts:1;"))
                        .header("x-request-id", "req-1"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/pagos/webhook").param("data.id", "987").param("type", "payment"))
                .andExpect(status().isUnauthorized());

        verify(pagoService, never()).procesarNotificacion(any(), any());
    }

    @Test
    void webhookUsaElCuerpoCuandoNoVieneEnLaUrl() throws Exception {
        String v1 = firma("request-id:req-2;ts:5;");

        mockMvc.perform(post("/pagos/webhook").header("x-signature", "ts=5,v1=" + v1).header("x-request-id", "req-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"payment\",\"data\":{\"id\":42}}"))
                .andExpect(status().isOk());

        verify(pagoService).procesarNotificacion("payment", "42");
    }

    @Test
    void unTokenVencidoNoBloqueaElWebhook() throws Exception {
        String v1 = firma("id:987;ts:9;");

        mockMvc.perform(post("/pagos/webhook").param("data.id", "987").param("type", "payment")
                        .header("Authorization", "Bearer token-invalido").header("x-signature", "ts=9,v1=" + v1))
                .andExpect(status().isOk());
    }
}
