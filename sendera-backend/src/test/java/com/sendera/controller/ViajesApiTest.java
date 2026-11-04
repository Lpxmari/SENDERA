package com.sendera.controller;

import com.sendera.config.SecurityConfig;
import com.sendera.dto.ActividadDto;
import com.sendera.dto.ViajeDto;
import com.sendera.exception.GlobalExceptionHandler;
import com.sendera.exception.HorarioSolapadoException;
import com.sendera.exception.RecursoNoEncontradoException;
import com.sendera.exception.ViajeInvalidoException;
import com.sendera.model.Usuario;
import com.sendera.security.CuentaActivaVerificador;
import com.sendera.security.JwtService;
import com.sendera.security.UsuarioActual;
import com.sendera.service.ActividadService;
import com.sendera.service.ViajeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RF-007/RF-009/RF-010: autenticación, identidad tomada del token, validación y mapeo de errores de /viajes y /actividades. */
@WebMvcTest({ViajeController.class, ActividadController.class})
@Import({SecurityConfig.class, GlobalExceptionHandler.class, JwtService.class})
@TestPropertySource(properties = {
        "app.jwt.secret=clave-de-pruebas-con-mas-de-treinta-y-dos-caracteres",
        "app.jwt.expiracion-horas=1",
        "app.cors.allowed-origins=http://localhost:5173"
})
class ViajesApiTest {

    private static final String CUERPO = """
            {"nombre":"Viaje al Quindío","fechaInicio":"2030-10-15","fechaFin":"2030-10-18",
             "actividades":[{"actividadId":3,"fecha":"2030-10-15","horaInicio":"09:00"}]}""";

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @MockBean ViajeService viajeService;
    @MockBean ActividadService actividadService;
    @MockBean CuentaActivaVerificador cuentaActiva;

    @BeforeEach
    void cuentaActiva() {
        when(cuentaActiva.estaActiva(7L)).thenReturn(true);
    }

    private String token() {
        return "Bearer " + jwtService.generar(Usuario.builder().id(7L).correo("ana@correo.com").rol("ROLE_CLIENTE").build());
    }

    private ViajeDto respuesta() {
        return ViajeDto.builder().id(1L).nombre("Viaje al Quindío").fechaInicio(LocalDate.of(2030, 10, 15))
                .fechaFin(LocalDate.of(2030, 10, 18)).actividades(List.of()).build();
    }

    @Test
    void losViajesExigenSesion() throws Exception {
        mockMvc.perform(get("/viajes")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/viajes/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/viajes").contentType(MediaType.APPLICATION_JSON).content(CUERPO)).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/viajes/1").contentType(MediaType.APPLICATION_JSON).content(CUERPO)).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/viajes/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void eliminarUsaElIdDeLaRutaYElDelTokenYDevuelve204() throws Exception {
        mockMvc.perform(delete("/viajes/1").header("Authorization", token())).andExpect(status().isNoContent());

        verify(viajeService).eliminar(1L, new UsuarioActual(7L, false));
    }

    @Test
    void eliminarUnViajeAjenoODesconocidoDevuelve404() throws Exception {
        doThrow(new RecursoNoEncontradoException("Itinerario no encontrado con id: 5")).when(viajeService).eliminar(eq(5L), any());

        mockMvc.perform(delete("/viajes/5").header("Authorization", token())).andExpect(status().isNotFound());
    }

    @Test
    void crearUsaElIdentificadorDelTokenYDevuelve201() throws Exception {
        when(viajeService.crear(any(), eq(new UsuarioActual(7L, false)))).thenReturn(respuesta());

        mockMvc.perform(post("/viajes").header("Authorization", token()).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Viaje al Quindío"));
    }

    @Test
    void ignoraUnPropietarioEnviadoEnElCuerpo() throws Exception {
        when(viajeService.crear(any(), eq(new UsuarioActual(7L, false)))).thenReturn(respuesta());

        mockMvc.perform(post("/viajes").header("Authorization", token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":99,\"nombre\":\"X\",\"fechaInicio\":\"2030-10-15\",\"fechaFin\":\"2030-10-18\"}"))
                .andExpect(status().isCreated());

        verify(viajeService).crear(any(), eq(new UsuarioActual(7L, false)));
    }

    @Test
    void datosInvalidosDevuelven400() throws Exception {
        for (String cuerpo : new String[]{
                "{}",
                "{\"nombre\":\"\",\"fechaInicio\":\"2030-10-15\",\"fechaFin\":\"2030-10-18\"}",
                "{\"nombre\":\"X\",\"fechaFin\":\"2030-10-18\"}",
                "{\"nombre\":\"X\",\"fechaInicio\":\"no-es-fecha\",\"fechaFin\":\"2030-10-18\"}",
                "{\"nombre\":\"X\",\"fechaInicio\":\"2030-10-15\",\"fechaFin\":\"2030-10-18\",\"actividades\":[{\"fecha\":\"2030-10-15\",\"horaInicio\":\"09:00\"}]}",
                "{\"nombre\":\"X\",\"fechaInicio\":\"2030-10-15\",\"fechaFin\":\"2030-10-18\",\"actividades\":[{\"actividadId\":1,\"fecha\":\"2030-10-15\",\"horaInicio\":\"25:99\"}]}"}) {
            mockMvc.perform(post("/viajes").header("Authorization", token()).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void unNombreDeMasDe120CaracteresDevuelve400() throws Exception {
        String largo = "a".repeat(121);

        mockMvc.perform(post("/viajes").header("Authorization", token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"" + largo + "\",\"fechaInicio\":\"2030-10-15\",\"fechaFin\":\"2030-10-18\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unSolapeDeHorarioDevuelve409ConElMensaje() throws Exception {
        when(viajeService.crear(any(), any())).thenThrow(new HorarioSolapadoException("Las actividades A y B se solapan."));

        mockMvc.perform(post("/viajes").header("Authorization", token()).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Las actividades A y B se solapan."));
    }

    @Test
    void fechasInvalidasDevuelven400ConElMensaje() throws Exception {
        when(viajeService.crear(any(), any())).thenThrow(new ViajeInvalidoException("No puedes usar fechas pasadas."));

        mockMvc.perform(post("/viajes").header("Authorization", token()).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No puedes usar fechas pasadas."));
    }

    @Test
    void unViajeAjenoODesconocidoDevuelve404() throws Exception {
        when(viajeService.obtenerPorId(eq(5L), any())).thenThrow(new RecursoNoEncontradoException("Itinerario no encontrado con id: 5"));
        when(viajeService.actualizar(eq(5L), any(), any())).thenThrow(new RecursoNoEncontradoException("Itinerario no encontrado con id: 5"));

        mockMvc.perform(get("/viajes/5").header("Authorization", token())).andExpect(status().isNotFound());
        mockMvc.perform(put("/viajes/5").header("Authorization", token()).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isNotFound());
    }

    @Test
    void actualizarUsaElIdDeLaRutaYElDelToken() throws Exception {
        when(viajeService.actualizar(eq(1L), any(), eq(new UsuarioActual(7L, false)))).thenReturn(respuesta());

        mockMvc.perform(put("/viajes/1").header("Authorization", token()).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isOk());
    }

    @Test
    void elCatalogoDeActividadesEsPublico() throws Exception {
        ActividadDto actividad = ActividadDto.builder().id(3L).destinoId(1L).destinoNombre("Filandia").ubicacion("Filandia, Quindío")
                .nombre("Recorrido").descripcion("d").duracionMinutos(180).dificultad("BAJA")
                .precioPorPersona(new BigDecimal("80000")).accesibilidad(true).build();
        when(actividadService.listar(null)).thenReturn(List.of(actividad));
        when(actividadService.listar(1L)).thenReturn(List.of(actividad));
        when(actividadService.obtenerPorId(3L)).thenReturn(actividad);

        mockMvc.perform(get("/actividades")).andExpect(status().isOk()).andExpect(jsonPath("$[0].nombre").value("Recorrido"));
        mockMvc.perform(get("/actividades").param("destinoId", "1")).andExpect(status().isOk());
        mockMvc.perform(get("/actividades/3")).andExpect(status().isOk()).andExpect(jsonPath("$.duracionMinutos").value(180));
    }

    @Test
    void elCatalogoNoAceptaEscrituraSinSesionNiParametrosInvalidos() throws Exception {
        mockMvc.perform(post("/actividades").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actividades").param("destinoId", "abc")).andExpect(status().isBadRequest());
    }
}
