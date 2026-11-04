package com.sendera.controller;

import com.sendera.config.SecurityConfig;
import com.sendera.dto.ReservaDto;
import com.sendera.exception.GlobalExceptionHandler;
import com.sendera.exception.ReservaNoPermitidaException;
import com.sendera.model.Usuario;
import com.sendera.security.CuentaActivaVerificador;
import com.sendera.security.JwtService;
import com.sendera.service.ReservaService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RF-012 / RNF-12: la fecha de la reserva es obligatoria, no puede ser pasada y los mensajes son claros. */
@WebMvcTest(ReservaController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, JwtService.class})
@TestPropertySource(properties = {
        "app.jwt.secret=clave-de-pruebas-con-mas-de-treinta-y-dos-caracteres",
        "app.jwt.expiracion-horas=1",
        "app.cors.allowed-origins=http://localhost:5173"
})
class ReservaApiTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @MockBean ReservaService reservaService;
    @MockBean CuentaActivaVerificador cuentaActiva;

    @BeforeEach
    void cuentaActiva() {
        when(cuentaActiva.estaActiva(7L)).thenReturn(true);
    }

    private String token() {
        return "Bearer " + jwtService.generar(Usuario.builder().id(7L).correo("a@b.com").rol("ROLE_CLIENTE").build());
    }

    @Test
    void fechaPasadaDevuelve400ConMensajeClaro() throws Exception {
        String ayer = LocalDate.now().minusDays(1).toString();

        mockMvc.perform(post("/reservas").header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itinerarioId\":5,\"fechaReserva\":\"" + ayer + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La fecha de la reserva no puede ser anterior a hoy."));

        verify(reservaService, never()).crear(any(), any());
    }

    @Test
    void fechaNulaOAusenteDevuelve400() throws Exception {
        for (String cuerpo : new String[]{"{\"itinerarioId\":5}", "{\"itinerarioId\":5,\"fechaReserva\":null}"}) {
            mockMvc.perform(post("/reservas").header("Authorization", token())
                            .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Elige la fecha de tu reserva."));
        }
        verify(reservaService, never()).crear(any(), any());
    }

    @Test
    void fechaDeHoyOFuturaCreaLaReservaConEstado201() throws Exception {
        LocalDate hoy = LocalDate.now();
        when(reservaService.crear(any(), any())).thenReturn(ReservaDto.builder().id(1L).itinerarioId(5L)
                .fechaReserva(hoy).montoTotal(new BigDecimal("195000.00")).estadoReserva("PENDIENTE").build());

        mockMvc.perform(post("/reservas").header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itinerarioId\":5,\"fechaReserva\":\"" + hoy + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoReserva").value("PENDIENTE"));
    }

    @Test
    void fechaFueraDelItinerarioSeTraduceEn409() throws Exception {
        when(reservaService.crear(any(), any())).thenThrow(new ReservaNoPermitidaException(
                "La fecha elegida está fuera del viaje. Elige un día entre el 10/10/2999 y el 15/10/2999."));

        mockMvc.perform(post("/reservas").header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itinerarioId\":5,\"fechaReserva\":\"2999-01-01\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "La fecha elegida está fuera del viaje. Elige un día entre el 10/10/2999 y el 15/10/2999."));
    }
}
