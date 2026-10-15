package com.sendera.service;

import com.sendera.dto.DestinoDto;
import com.sendera.dto.FiltroDestinoDto;
import com.sendera.model.Destino;
import com.sendera.repository.DestinoRepository;
import com.sendera.repository.ItinerarioRepository;
import com.sendera.service.impl.DestinoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DestinoServiceImplTest {

    @Mock
    private DestinoRepository destinoRepository;

    @Mock
    private ItinerarioRepository itinerarioRepository;

    @InjectMocks
    private DestinoServiceImpl destinoService;

    private Destino destinoEjemplo;

    @BeforeEach
    void setUp() {
        destinoEjemplo = Destino.builder()
                .id(1L)
                .nombre("Valle de Cocora")
                .descripcion("Palmas de cera")
                .ubicacion("Salento")
                .precioBase(new BigDecimal("85000.00"))
                .dificultad("BAJA")
                .accesibilidad(true)
                .build();
    }

    @Test
    void listarDestinos_DeberiaRetornarListaDeDestinos() {
        when(destinoRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(destinoEjemplo));

        FiltroDestinoDto filtro = new FiltroDestinoDto();
        List<DestinoDto> resultado = destinoService.listarDestinos(filtro);

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("Valle de Cocora", resultado.get(0).getNombre());
        assertEquals("BAJA", resultado.get(0).getDificultad());
        assertTrue(resultado.get(0).getAccesibilidad());
        verify(destinoRepository, times(1)).findAll(any(Specification.class), any(Sort.class));
    }
}