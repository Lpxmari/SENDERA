package com.sendera.controller;

import com.sendera.config.SecurityConfig;
import com.sendera.exception.GlobalExceptionHandler;
import com.sendera.model.Destino;
import com.sendera.repository.DestinoRepository;
import com.sendera.repository.ItinerarioRepository;
import com.sendera.security.JwtService;
import com.sendera.service.AuditoriaService;
import com.sendera.service.impl.DestinoServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** OB-01: parámetros page/size de GET /destinos, forma de la respuesta y 400 con el formato ApiError (servicio real). */
@WebMvcTest(DestinoController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, JwtService.class, DestinoServiceImpl.class})
@TestPropertySource(properties = {
        "app.jwt.secret=clave-de-pruebas-con-mas-de-treinta-y-dos-caracteres",
        "app.jwt.expiracion-horas=1",
        "app.cors.allowed-origins=http://localhost:5173"
})
class DestinoPaginacionApiTest {

    @Autowired MockMvc mockMvc;
    @MockBean DestinoRepository destinoRepository;
    @MockBean ItinerarioRepository itinerarioRepository;
    @MockBean AuditoriaService auditoriaService;

    private Destino destino(long id) {
        return Destino.builder().id(id).nombre("Destino " + id).descripcion("d").ubicacion("u")
                .precioBase(new BigDecimal("1000")).dificultad("BAJA").accesibilidad(true).fotosUrl(List.of()).build();
    }

    private Pageable pageableUsado() {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(destinoRepository).findAll(any(Specification.class), captor.capture());
        return captor.getValue();
    }

    @Test
    void sinParametrosUsaLaPagina0Y12PorPagina() throws Exception {
        when(destinoRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(destino(1)), PageRequest.of(0, 12), 1));

        mockMvc.perform(get("/destinos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido.length()").value(1))
                .andExpect(jsonPath("$.contenido[0].nombre").value("Destino 1"))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamano").value(12))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.totalPaginas").value(1));

        Pageable pageable = pageableUsado();
        assertEquals(0, pageable.getPageNumber());
        assertEquals(12, pageable.getPageSize());
    }

    @Test
    void laPagina1ConSizeExplicitoLlegaALaConsulta() throws Exception {
        when(destinoRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(destino(6)), PageRequest.of(1, 5), 6));

        mockMvc.perform(get("/destinos").param("page", "1").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.tamano").value(5))
                .andExpect(jsonPath("$.totalElementos").value(6))
                .andExpect(jsonPath("$.totalPaginas").value(2));

        Pageable pageable = pageableUsado();
        assertEquals(1, pageable.getPageNumber());
        assertEquals(5, pageable.getPageSize());
    }

    @Test
    void unSizeMayorQue50SeLimitaA50SinError() throws Exception {
        when(destinoRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 50), 0));

        mockMvc.perform(get("/destinos").param("size", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tamano").value(50));

        assertEquals(50, pageableUsado().getPageSize());
    }

    @Test
    void sizeCeroDevuelve400ConMensajeEnEspanolYFormatoApiError() throws Exception {
        mockMvc.perform(get("/destinos").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("El tamaño de página debe ser al menos 1."))
                .andExpect(jsonPath("$.path").value("/destinos"))
                .andExpect(jsonPath("$.timestamp").exists());
        verifyNoInteractions(destinoRepository);
    }

    @Test
    void paginaNegativaDevuelve400ConMensajeEnEspanol() throws Exception {
        mockMvc.perform(get("/destinos").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El número de página debe ser 0 o mayor."));
        verifyNoInteractions(destinoRepository);
    }

    @Test
    void unPageOSizeQueNoSonNumerosDevuelven400() throws Exception {
        mockMvc.perform(get("/destinos").param("page", "uno")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/destinos").param("size", "muchos")).andExpect(status().isBadRequest());
    }
}
