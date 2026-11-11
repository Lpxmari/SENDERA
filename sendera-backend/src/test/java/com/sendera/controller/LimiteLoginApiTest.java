package com.sendera.controller;

import com.sendera.config.SecurityConfig;
import com.sendera.dto.AuthRespuestaDto;
import com.sendera.exception.CredencialesInvalidasException;
import com.sendera.exception.GlobalExceptionHandler;
import com.sendera.security.JwtService;
import com.sendera.service.AuthService;
import com.sendera.service.DestinoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Límite de intentos de POST /auth/login (auditoría OB-02, OM-04, OM-07): 5 por minuto por IP + correo y 20 por IP.
 * Cada prueba usa IP propias para no compartir cuenta con las demás (el servicio es un singleton del contexto).
 */
@WebMvcTest({AuthController.class, DestinoController.class})
@Import({SecurityConfig.class, GlobalExceptionHandler.class, JwtService.class})
@TestPropertySource(properties = {
        "app.jwt.secret=clave-de-pruebas-con-mas-de-treinta-y-dos-caracteres",
        "app.jwt.expiracion-horas=1",
        "app.cors.allowed-origins=http://localhost:5173",
        "app.seguridad.login.intentos-por-minuto=5",
        "app.seguridad.login.intentos-por-ip-por-minuto=20"
})
class LimiteLoginApiTest {

    private static final String MENSAJE_429 =
            "Demasiados intentos de inicio de sesión. Espera un minuto e inténtalo de nuevo.";

    @Autowired
    MockMvc mockMvc;
    @MockBean
    AuthService authService;
    @MockBean
    DestinoService destinoService;

    private static RequestPostProcessor desdeIp(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }

    private ResultActions login(String correo, String ip) throws Exception {
        return mockMvc.perform(post("/auth/login").with(desdeIp(ip)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"" + correo + "\",\"contrasena\":\"incorrecta\"}"));
    }

    @Test
    void losPrimerosCincoIntentosPasanYElSextoDevuelve429ConRetryAfter() throws Exception {
        when(authService.iniciarSesion(any())).thenThrow(new CredencialesInvalidasException());

        for (int i = 1; i <= 5; i++) {
            login("ana@correo.com", "10.1.0.1").andExpect(status().isUnauthorized());
        }

        login("ana@correo.com", "10.1.0.1")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(header().string("Retry-After", org.hamcrest.Matchers.matchesPattern("[1-9]\\d?|60")))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("Too Many Requests"))
                .andExpect(jsonPath("$.message").value(MENSAJE_429))
                .andExpect(jsonPath("$.path").value("/auth/login"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void unIntentoBloqueadoNoLlegaAlServicioDeAutenticacion() throws Exception {
        for (int i = 1; i <= 6; i++) {
            login("bloqueado@correo.com", "10.1.0.2");
        }

        // 5 intentos llegan al servicio; el 6.º lo corta el filtro.
        verify(authService, org.mockito.Mockito.times(5)).iniciarSesion(any());
    }

    @Test
    void elCorreoSeNormalizaConTrimYMinusculas() throws Exception {
        for (int i = 1; i <= 5; i++) {
            login("  Mezcla@Correo.COM ", "10.1.0.3").andExpect(status().is(org.hamcrest.Matchers.not(429)));
        }

        login("mezcla@correo.com", "10.1.0.3").andExpect(status().isTooManyRequests());
    }

    @Test
    void otroCorreoDesdeLaMismaIpNoSeBloqueaHastaElTopePorIp() throws Exception {
        for (int i = 1; i <= 6; i++) {
            login("uno@correo.com", "10.1.0.4");
        }
        login("uno@correo.com", "10.1.0.4").andExpect(status().isTooManyRequests());

        // El correo bloqueado no gastó cupo de la IP: caben 20 intentos en total, 5 ya usados por "uno".
        for (int i = 1; i <= 5; i++) {
            login("dos@correo.com", "10.1.0.4").andExpect(status().is(org.hamcrest.Matchers.not(429)));
        }
        for (int i = 1; i <= 5; i++) {
            login("tres@correo.com", "10.1.0.4").andExpect(status().is(org.hamcrest.Matchers.not(429)));
        }
        for (int i = 1; i <= 5; i++) {
            login("cuatro@correo.com", "10.1.0.4").andExpect(status().is(org.hamcrest.Matchers.not(429)));
        }
        // Van 20 intentos aceptados desde la IP: un correo nuevo ya choca con el tope por IP.
        login("cinco@correo.com", "10.1.0.4")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void otraIpNoSeVeAfectada() throws Exception {
        for (int i = 1; i <= 6; i++) {
            login("compartido@correo.com", "10.1.0.5");
        }
        login("compartido@correo.com", "10.1.0.5").andExpect(status().isTooManyRequests());

        login("compartido@correo.com", "10.1.0.6").andExpect(status().is(org.hamcrest.Matchers.not(429)));
    }

    @Test
    void laIpSeTomaDelPrimerValorDeXForwardedFor() throws Exception {
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/auth/login").with(desdeIp("10.9.9.9"))
                    .header("X-Forwarded-For", "203.0.113.7, 10.0.0.1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"correo\":\"proxy@correo.com\",\"contrasena\":\"x\"}"));
        }

        // Misma IP reenviada aunque la conexión venga de otro proxy: bloqueada.
        mockMvc.perform(post("/auth/login").with(desdeIp("10.9.9.10"))
                        .header("X-Forwarded-For", "203.0.113.7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"proxy@correo.com\",\"contrasena\":\"x\"}"))
                .andExpect(status().isTooManyRequests());
        // Otra IP de cliente detrás del mismo proxy: no se ve afectada.
        mockMvc.perform(post("/auth/login").with(desdeIp("10.9.9.9"))
                        .header("X-Forwarded-For", "203.0.113.8")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"proxy@correo.com\",\"contrasena\":\"x\"}"))
                .andExpect(status().is(org.hamcrest.Matchers.not(429)));
    }

    @Test
    void unXForwardedForInvalidoCaeEnLaIpDeLaConexion() throws Exception {
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/auth/login").with(desdeIp("10.1.0.7"))
                    .header("X-Forwarded-For", "no-es-una-ip<script>")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"correo\":\"raro@correo.com\",\"contrasena\":\"x\"}"));
        }

        login("raro@correo.com", "10.1.0.7").andExpect(status().isTooManyRequests());
    }

    @Test
    void unCuerpoIlegibleNoRompeElFiltroYElControladorResponde400() throws Exception {
        mockMvc.perform(post("/auth/login").with(desdeIp("10.1.0.8")).contentType(MediaType.APPLICATION_JSON)
                        .content("{esto no es json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void elCuerpoLlegaCompletoAlControladorDespuesDelFiltro() throws Exception {
        when(authService.iniciarSesion(any())).thenReturn(new AuthRespuestaDto());

        login("cuerpo@correo.com", "10.1.0.9").andExpect(status().isOk());

        org.mockito.ArgumentCaptor<com.sendera.dto.LoginDto> captor =
                org.mockito.ArgumentCaptor.forClass(com.sendera.dto.LoginDto.class);
        verify(authService).iniciarSesion(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("cuerpo@correo.com", captor.getValue().getCorreo());
        org.junit.jupiter.api.Assertions.assertEquals("incorrecta", captor.getValue().getContrasena());
    }

    @Test
    void elLimiteNoAfectaOtrosEndpointsDeLaMismaIp() throws Exception {
        for (int i = 1; i <= 25; i++) {
            login("agotar@correo.com", "10.1.0.10");
        }
        login("agotar@correo.com", "10.1.0.10").andExpect(status().isTooManyRequests());

        mockMvc.perform(get("/destinos").with(desdeIp("10.1.0.10"))).andExpect(status().isOk());
        for (int i = 1; i <= 8; i++) {
            mockMvc.perform(post("/auth/registro").with(desdeIp("10.1.0.10")).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nombre\":\"Ana\",\"apellidos\":\"Pérez\",\"correo\":\"ana@correo.com\","
                                    + "\"contrasena\":\"Clave1234\"}"))
                    .andExpect(status().is(org.hamcrest.Matchers.not(429)));
        }
    }

    @Test
    void elRetryAfterQuedaExpuestoParaElFrontendPorCors() throws Exception {
        for (int i = 1; i <= 5; i++) {
            login("cors@correo.com", "10.1.0.11");
        }

        mockMvc.perform(post("/auth/login").with(desdeIp("10.1.0.11")).header("Origin", "http://localhost:5173")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"cors@correo.com\",\"contrasena\":\"x\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Expose-Headers", containsString("Retry-After")));
    }
}
