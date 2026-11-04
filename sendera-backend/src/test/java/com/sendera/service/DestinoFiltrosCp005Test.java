package com.sendera.service;

import com.sendera.dto.FiltroDestinoDto;
import com.sendera.exception.FiltroInvalidoException;
import com.sendera.repository.DestinoRepository;
import com.sendera.repository.ItinerarioRepository;
import com.sendera.service.impl.DestinoServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * CP-005 (RF-006, RF-008, RNF-26): validación de los filtros de destinos (precio, dificultad, accesibilidad).
 * Cubre las clases válidas e inválidas y los valores límite; el servicio rechaza antes de consultar la base.
 */
@ExtendWith(MockitoExtension.class)
class DestinoFiltrosCp005Test {

    @Mock
    private DestinoRepository destinoRepository;
    @Mock
    private ItinerarioRepository itinerarioRepository;
    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private DestinoServiceImpl destinoService;

    private void aceptaElFiltro(FiltroDestinoDto filtro) {
        when(destinoRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of());
        assertEquals(List.of(), destinoService.listarDestinos(filtro));
        verify(destinoRepository).findAll(any(Specification.class), any(Sort.class));
    }

    private FiltroInvalidoException rechazaElFiltro(FiltroDestinoDto filtro) {
        FiltroInvalidoException error = assertThrows(FiltroInvalidoException.class, () -> destinoService.listarDestinos(filtro));
        verifyNoInteractions(destinoRepository);
        return error;
    }

    @Test
    void dificultadValidaEnMayusculasSeAcepta() {
        FiltroDestinoDto filtro = FiltroDestinoDto.builder().dificultad("BAJA").build();
        aceptaElFiltro(filtro);
        assertEquals("BAJA", filtro.getDificultad());
    }

    @Test
    void dificultadEnMinusculasYConEspaciosSeNormaliza() {
        FiltroDestinoDto filtro = FiltroDestinoDto.builder().dificultad(" baja ").build();
        aceptaElFiltro(filtro);
        assertEquals("BAJA", filtro.getDificultad());
    }

    @Test
    void dificultadAltaSeAcepta() {
        FiltroDestinoDto filtro = FiltroDestinoDto.builder().dificultad("ALTA").build();
        aceptaElFiltro(filtro);
        assertEquals("ALTA", filtro.getDificultad());
    }

    @Test
    void accesibilidadVerdaderaSeAcepta() {
        aceptaElFiltro(FiltroDestinoDto.builder().accesibilidad(true).build());
    }

    @Test
    void accesibilidadFalsaSeAcepta() {
        aceptaElFiltro(FiltroDestinoDto.builder().accesibilidad(false).build());
    }

    @Test
    void rangoDePrecioDeCeroACuatrocientosMilSeAcepta() {
        aceptaElFiltro(FiltroDestinoDto.builder().precioMin(BigDecimal.ZERO).precioMax(new BigDecimal("500000")).build());
    }

    @Test
    void precioMinimoIgualAlMaximoEsUnLimiteValido() {
        aceptaElFiltro(FiltroDestinoDto.builder().precioMin(new BigDecimal("500000")).precioMax(new BigDecimal("500000")).build());
    }

    @Test
    void sinFiltrosSeAcepta() {
        aceptaElFiltro(new FiltroDestinoDto());
    }

    @Test
    void filtrosCombinadosSeAceptan() {
        FiltroDestinoDto filtro = FiltroDestinoDto.builder().dificultad("baja").accesibilidad(true)
                .precioMin(BigDecimal.ZERO).precioMax(new BigDecimal("500000")).build();
        aceptaElFiltro(filtro);
        assertEquals("BAJA", filtro.getDificultad());
    }

    @Test
    void dificultadExtremaSeRechazaConMensajeClaro() {
        FiltroInvalidoException error = rechazaElFiltro(FiltroDestinoDto.builder().dificultad("EXTREMA").build());
        assertEquals("La dificultad debe ser BAJA, MODERADA o ALTA.", error.getMessage());
    }

    @Test
    void precioMinimoNegativoSeRechazaConMensajeClaro() {
        FiltroInvalidoException error = rechazaElFiltro(FiltroDestinoDto.builder().precioMin(new BigDecimal("-1")).build());
        assertEquals("Los precios no pueden ser negativos.", error.getMessage());
    }

    @Test
    void precioMaximoNegativoSeRechazaConMensajeClaro() {
        FiltroInvalidoException error = rechazaElFiltro(FiltroDestinoDto.builder().precioMax(new BigDecimal("-1")).build());
        assertEquals("Los precios no pueden ser negativos.", error.getMessage());
    }

    @Test
    void precioMinimoMayorQueElMaximoSeRechazaConMensajeClaro() {
        FiltroInvalidoException error = rechazaElFiltro(
                FiltroDestinoDto.builder().precioMin(new BigDecimal("600000")).precioMax(new BigDecimal("500000")).build());
        assertEquals("El precio mínimo no puede ser mayor que el máximo.", error.getMessage());
    }

    @Test
    void unFiltroInvalidoRechazaLaConsultaAunqueLosOtrosSeanValidos() {
        rechazaElFiltro(FiltroDestinoDto.builder().dificultad("EXTREMA").accesibilidad(true)
                .precioMin(BigDecimal.ZERO).precioMax(new BigDecimal("500000")).build());
    }
}
