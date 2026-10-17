package com.sendera.controller;

import com.sendera.config.SecurityConfig;
import com.sendera.dto.PaginaDto;
import com.sendera.dto.ReservaAdminDto;
import com.sendera.dto.ResenaAdminDto;
import com.sendera.exception.GlobalExceptionHandler;
import com.sendera.exception.RecursoNoEncontradoException;
import com.sendera.exception.ReservaNoPermitidaException;
import com.sendera.model.Usuario;
import com.sendera.security.CuentaActivaVerificador;
import com.sendera.security.JwtService;
import com.sendera.security.UsuarioActual;
import com.sendera.service.ReservaAdminService;
import com.sendera.service.ResenaAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RF-005/RF-012/RF-013: reservas y reseñas administrativas exigen ROLE_ADMIN y toman la identidad del token. */
@WebMvcTest({ReservaAdminController.class, ResenaAdminController.class})
@Import({SecurityConfig.class, GlobalExceptionHandler.class, JwtService.class})
@TestPropertySource(properties = {
        "app.jwt.secret=clave-de-pruebas-con-mas-de-treinta-y-dos-caracteres",
        "app.jwt.expiracion-horas=1",
        "app.cors.allowed-origins=http://localhost:5173"
})
class AdminReservasResenasApiTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @MockBean ReservaAdminService reservaAdminService;
    @MockBean ResenaAdminService resenaAdminService;
    @MockBean CuentaActivaVerificador cuentaActiva;

    @BeforeEach
    void cuentasActivas() {
        when(cuentaActiva.estaActiva(7L)).thenReturn(true);
    }

    private String token(String rol) {
        return "Bearer " + jwtService.generar(Usuario.builder().id(7L).correo("a@b.com").rol(rol).build());
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/admin/reservas")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/admin/resenas")).andExpect(status().isUnauthorized());
    }

    @Test
    void comoClienteDevuelve403EnTodasLasRutas() throws Exception {
        String cliente = token("ROLE_CLIENTE");
        mockMvc.perform(get("/admin/reservas").header("Authorization", cliente)).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/reservas/1").header("Authorization", cliente)).andExpect(status().isForbidden());
        mockMvc.perform(patch("/admin/reservas/1/estado").header("Authorization", cliente)
                .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"CANCELADA\"}")).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/resenas").header("Authorization", cliente)).andExpect(status().isForbidden());
        mockMvc.perform(delete("/admin/resenas/1").header("Authorization", cliente)).andExpect(status().isForbidden());
    }

    @Test
    void comoAdminListaReservasConFiltros() throws Exception {
        when(reservaAdminService.listar("CONFIRMADA", "ana", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), 1, 5))
                .thenReturn(PaginaDto.<ReservaAdminDto>builder()
                        .contenido(List.of(ReservaAdminDto.builder().id(3L).usuarioNombre("Ana Pérez").estadoReserva("CONFIRMADA").build()))
                        .pagina(1).tamano(5).total(6).build());

        mockMvc.perform(get("/admin/reservas")
                        .param("estado", "CONFIRMADA").param("q", "ana").param("desde", "2026-10-01")
                        .param("hasta", "2026-10-31").param("pagina", "1").param("tamano", "5")
                        .header("Authorization", token("ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(6))
                .andExpect(jsonPath("$.contenido[0].usuarioNombre").value("Ana Pérez"));
    }

    @Test
    void fechaConFormatoInvalidoDevuelve400() throws Exception {
        mockMvc.perform(get("/admin/reservas").param("desde", "ayer").header("Authorization", token("ROLE_ADMIN")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cambiarEstadoTomaLaIdentidadDelToken() throws Exception {
        when(reservaAdminService.cambiarEstado(eq(3L), any(), any()))
                .thenReturn(ReservaAdminDto.builder().id(3L).estadoReserva("CANCELADA").build());

        mockMvc.perform(patch("/admin/reservas/3/estado").header("Authorization", token("ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"CANCELADA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoReserva").value("CANCELADA"));

        verify(reservaAdminService).cambiarEstado(eq(3L), any(), eq(new UsuarioActual(7L, true)));
    }

    @Test
    void estadoInvalidoDevuelve400() throws Exception {
        for (String cuerpo : List.of("{\"estado\":\"PENDIENTE\"}", "{\"estado\":\"ENVIADA\"}", "{}")) {
            mockMvc.perform(patch("/admin/reservas/3/estado").header("Authorization", token("ROLE_ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void transicionNoPermitidaDevuelve409() throws Exception {
        when(reservaAdminService.cambiarEstado(eq(3L), any(), any()))
                .thenThrow(new ReservaNoPermitidaException("La reserva ya fue cancelada y no puede cambiar de estado."));

        mockMvc.perform(patch("/admin/reservas/3/estado").header("Authorization", token("ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"CONFIRMADA\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("La reserva ya fue cancelada y no puede cambiar de estado."));
    }

    @Test
    void comoAdminListaResenas() throws Exception {
        when(resenaAdminService.listar(4L, 2, "sucio", 0, 20)).thenReturn(PaginaDto.<ResenaAdminDto>builder()
                .contenido(List.of(ResenaAdminDto.builder().id(9L).comentario("Sucio").calificacion(1).tieneImagen(true).build()))
                .pagina(0).tamano(20).total(1).build());

        mockMvc.perform(get("/admin/resenas").param("destinoId", "4").param("calificacionMax", "2").param("q", "sucio")
                        .header("Authorization", token("ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].tieneImagen").value(true));
    }

    @Test
    void eliminarResenaDevuelve204YUsaLaIdentidadDelToken() throws Exception {
        mockMvc.perform(delete("/admin/resenas/9").header("Authorization", token("ROLE_ADMIN")))
                .andExpect(status().isNoContent());

        verify(resenaAdminService).eliminar(9L, new UsuarioActual(7L, true));
    }

    @Test
    void eliminarResenaInexistenteDevuelve404() throws Exception {
        doThrow(new RecursoNoEncontradoException("Reseña no encontrada con id: 9"))
                .when(resenaAdminService).eliminar(eq(9L), any());

        mockMvc.perform(delete("/admin/resenas/9").header("Authorization", token("ROLE_ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    void cuentaDesactivadaNoPuedeModerar() throws Exception {
        when(cuentaActiva.estaActiva(7L)).thenReturn(false);

        mockMvc.perform(delete("/admin/resenas/9").header("Authorization", token("ROLE_ADMIN")))
                .andExpect(status().isUnauthorized());
    }
}
