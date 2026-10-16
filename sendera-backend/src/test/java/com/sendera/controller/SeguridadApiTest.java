package com.sendera.controller;

import com.sendera.config.SecurityConfig;
import com.sendera.dto.CrearResenaDto;
import com.sendera.dto.ResenaDto;
import com.sendera.exception.GlobalExceptionHandler;
import com.sendera.controller.ResenaController;
import com.sendera.model.Usuario;
import com.sendera.security.JwtService;
import com.sendera.security.UsuarioActual;
import com.sendera.service.DestinoService;
import com.sendera.service.ReservaService;
import com.sendera.service.ResenaService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RF-005: autenticación, autorización por rol y formato uniforme de error (sin base de datos). */
@WebMvcTest({DestinoController.class, ReservaController.class, ResenaController.class})
@Import({SecurityConfig.class, GlobalExceptionHandler.class, JwtService.class})
@TestPropertySource(properties = {
        "app.jwt.secret=clave-de-pruebas-con-mas-de-treinta-y-dos-caracteres",
        "app.jwt.expiracion-horas=1",
        "app.cors.allowed-origins=http://localhost:5173"
})
class SeguridadApiTest {

    private static final String DESTINO_VALIDO = """
            {"nombre":"Valle","descripcion":"Paisaje","ubicacion":"Salento","precioBase":85000,
             "dificultad":"BAJA","accesibilidad":true,"fotosUrl":[]}""";

    @Autowired
    MockMvc mockMvc;
    @Autowired
    JwtService jwtService;
    @Autowired
    JwtEncoder jwtEncoder;
    @MockBean
    DestinoService destinoService;
    @MockBean
    ReservaService reservaService;
    @MockBean
    ResenaService resenaService;

    private String token(String rol) {
        return jwtService.generar(Usuario.builder().id(1L).correo("a@b.com").rol(rol).build());
    }

    private String tokenConEmisor(String emisor, Instant expira) {
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(emisor).subject("a@b.com")
                .issuedAt(expira.minusSeconds(3600)).expiresAt(expira)
                .claim("roles", List.of("ROLE_ADMIN")).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    @Test
    void consultarDestinosEsPublico() throws Exception {
        mockMvc.perform(get("/destinos")).andExpect(status().isOk());
    }

    @Test
    void listarResenasEsPublico() throws Exception {
        mockMvc.perform(get("/destinos/1/resenas")).andExpect(status().isOk());
    }

    @Test
    void publicarResenaSinTokenDevuelve401() throws Exception {
        mockMvc.perform(multipart("/destinos/1/resenas")
                        .param("comentario", "Un lugar hermoso")
                        .param("calificacion", "5"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicarResenaAsociaElUsuarioDelJwt() throws Exception {
        when(resenaService.crear(anyLong(), any(CrearResenaDto.class), isNull(), any()))
                .thenReturn(ResenaDto.builder()
                        .id(7L)
                        .destinoId(1L)
                        .comentario("Un lugar hermoso")
                        .calificacion(5)
                        .autor("Ana Pérez")
                        .build());

        mockMvc.perform(multipart("/destinos/1/resenas")
                        .header("Authorization", "Bearer " + token("ROLE_CLIENTE"))
                        .param("comentario", "Un lugar hermoso")
                        .param("calificacion", "5"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.autor").value("Ana Pérez"))
                .andExpect(jsonPath("$.calificacion").value(5));

        verify(resenaService).crear(eq(1L), any(CrearResenaDto.class), isNull(),
                eq(new UsuarioActual(1L, false)));
    }

    @Test
    void consultaPublicaIgnoraTokenVencido() throws Exception {
        String vencido = tokenConEmisor("sendera", Instant.now().minusSeconds(60));
        mockMvc.perform(get("/destinos").header("Authorization", "Bearer " + vencido))
                .andExpect(status().isOk());
    }

    @Test
    void crearDestinoSinTokenDevuelve401ConFormatoApiError() throws Exception {
        mockMvc.perform(post("/destinos").contentType(MediaType.APPLICATION_JSON).content(DESTINO_VALIDO))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/destinos"));
    }

    @Test
    void crearDestinoComoClienteDevuelve403() throws Exception {
        mockMvc.perform(post("/destinos").header("Authorization", "Bearer " + token("ROLE_CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON).content(DESTINO_VALIDO))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void eliminarDestinoComoClienteDevuelve403() throws Exception {
        mockMvc.perform(delete("/destinos/1").header("Authorization", "Bearer " + token("ROLE_CLIENTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void crearDestinoComoAdminDevuelve201() throws Exception {
        mockMvc.perform(post("/destinos").header("Authorization", "Bearer " + token("ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(DESTINO_VALIDO))
                .andExpect(status().isCreated());
    }

    @Test
    void tokenVencidoEnRutaProtegidaDevuelve401() throws Exception {
        String vencido = tokenConEmisor("sendera", Instant.now().minusSeconds(60));
        mockMvc.perform(delete("/destinos/1").header("Authorization", "Bearer " + vencido))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenManipuladoDevuelve401() throws Exception {
        String manipulado = token("ROLE_ADMIN") + "x";
        mockMvc.perform(delete("/destinos/1").header("Authorization", "Bearer " + manipulado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenConEmisorDistintoDevuelve401() throws Exception {
        String ajeno = tokenConEmisor("otro-emisor", Instant.now().plusSeconds(600));
        mockMvc.perform(delete("/destinos/1").header("Authorization", "Bearer " + ajeno))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void reservasRequierenSesion() throws Exception {
        mockMvc.perform(get("/reservas")).andExpect(status().isUnauthorized());
    }

    @Test
    void elSolicitanteSeTomaDelTokenYNoDeLaPeticion() throws Exception {
        // token de un cliente con id 1 (ver token()); el parámetro usuarioId de la query no debe influir
        mockMvc.perform(get("/reservas?usuarioId=99").header("Authorization", "Bearer " + token("ROLE_CLIENTE")))
                .andExpect(status().isOk());

        verify(reservaService).listar(new UsuarioActual(1L, false));
    }

    @Test
    void errorInesperadoDevuelve500SinDetallesInternos() throws Exception {
        when(reservaService.obtenerPorId(anyLong(), any())).thenThrow(new IllegalStateException("fallo SQL interno"));
        mockMvc.perform(get("/reservas/1").header("Authorization", "Bearer " + token("ROLE_CLIENTE")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Ocurrió un error inesperado. Intenta de nuevo más tarde."))
                .andExpect(content().string(Matchers.not(Matchers.containsString("SQL"))));
    }

    @Test
    void crearReservaSinItinerarioDevuelve400() throws Exception {
        mockMvc.perform(post("/reservas").header("Authorization", "Bearer " + token("ROLE_CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Selecciona un itinerario para reservar."));
    }

    @Test
    void metodoNoPermitidoNoSeConvierteEn500() throws Exception {
        mockMvc.perform(put("/reservas").header("Authorization", "Bearer " + token("ROLE_CLIENTE")))
                .andExpect(status().isMethodNotAllowed());
    }
}
