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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    @Mock
    private AuditoriaService auditoriaService;

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
        when(destinoRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(destinoEjemplo), PageRequest.of(0, 12), 1));

        FiltroDestinoDto filtro = new FiltroDestinoDto();
        com.sendera.dto.PaginaResultadosDto<DestinoDto> pagina = destinoService.listarDestinos(filtro, 0, 12);

        assertNotNull(pagina);
        List<DestinoDto> resultado = pagina.getContenido();
        assertEquals(1, resultado.size());
        assertEquals("Valle de Cocora", resultado.get(0).getNombre());
        assertEquals("BAJA", resultado.get(0).getDificultad());
        assertTrue(resultado.get(0).getAccesibilidad());
        verify(destinoRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void listarDestinos_PideLaPaginaALaBaseOrdenadaPorIdYCalculaLosTotales() {
        when(destinoRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(destinoEjemplo), PageRequest.of(1, 5), 11));

        com.sendera.dto.PaginaResultadosDto<DestinoDto> pagina = destinoService.listarDestinos(new FiltroDestinoDto(), 1, 5);

        org.mockito.ArgumentCaptor<Pageable> captor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(destinoRepository).findAll(any(Specification.class), captor.capture());
        assertEquals(1, captor.getValue().getPageNumber());
        assertEquals(5, captor.getValue().getPageSize());
        assertEquals(Sort.by(Sort.Direction.ASC, "id"), captor.getValue().getSort());
        assertEquals(1, pagina.getPagina());
        assertEquals(5, pagina.getTamano());
        assertEquals(11, pagina.getTotalElementos());
        assertEquals(3, pagina.getTotalPaginas());
    }

    @Test
    void listarDestinos_LimitaElTamanoA50() {
        when(destinoRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 50), 0));

        com.sendera.dto.PaginaResultadosDto<DestinoDto> pagina = destinoService.listarDestinos(new FiltroDestinoDto(), 0, 500);

        org.mockito.ArgumentCaptor<Pageable> captor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(destinoRepository).findAll(any(Specification.class), captor.capture());
        assertEquals(50, captor.getValue().getPageSize());
        assertEquals(50, pagina.getTamano());
    }

    @Test
    void listarDestinos_RechazaTamanoMenorQue1YPaginaNegativaSinConsultarLaBase() {
        com.sendera.exception.FiltroInvalidoException tamano = assertThrows(com.sendera.exception.FiltroInvalidoException.class,
                () -> destinoService.listarDestinos(new FiltroDestinoDto(), 0, 0));
        assertEquals("El tamaño de página debe ser al menos 1.", tamano.getMessage());
        com.sendera.exception.FiltroInvalidoException pagina = assertThrows(com.sendera.exception.FiltroInvalidoException.class,
                () -> destinoService.listarDestinos(new FiltroDestinoDto(), -1, 12));
        assertEquals("El número de página debe ser 0 o mayor.", pagina.getMessage());
        verifyNoInteractions(destinoRepository);
    }

    @Test
    void crearDestino_DeberiaAuditarLaAccionDelAdministrador() {
        com.sendera.dto.CrearDestinoDto dto = new com.sendera.dto.CrearDestinoDto();
        dto.setNombre("Valle de Cocora");
        dto.setDescripcion("Palmas de cera");
        dto.setUbicacion("Salento");
        dto.setPrecioBase(new BigDecimal("85000.00"));
        dto.setDificultad("BAJA");
        dto.setAccesibilidad(true);
        dto.setFotosUrl(List.of());
        when(destinoRepository.save(any(Destino.class))).thenReturn(destinoEjemplo);
        com.sendera.security.UsuarioActual admin = new com.sendera.security.UsuarioActual(9L, true);

        destinoService.crearDestino(dto, admin);

        verify(auditoriaService).registrar(admin, "DESTINO_CREAR", "destino", 1L, "Valle de Cocora");
    }

    @Test
    void listarItinerariosDisponibles_ConvierteADtoConLosCamposDelContrato() {
        when(destinoRepository.findById(1L)).thenReturn(java.util.Optional.of(destinoEjemplo));
        com.sendera.model.Itinerario itinerario = com.sendera.model.Itinerario.builder().id(5L).nombre("Ruta del Café")
                .fechaInicio(java.time.LocalDate.of(2026, 11, 18)).fechaFin(java.time.LocalDate.of(2026, 11, 19))
                .precioBase(new BigDecimal("195000.00")).cuposDisponibles(8).build();
        when(itinerarioRepository.findByDestinoIdAndCuposDisponiblesGreaterThanAndFechaFinGreaterThanEqualOrderByFechaInicioAsc(
                eq(1L), eq(0), any(java.time.LocalDate.class))).thenReturn(List.of(itinerario));

        List<com.sendera.dto.ItinerarioDto> resultado = destinoService.listarItinerariosDisponibles(1L);

        assertEquals(1, resultado.size());
        assertEquals(5L, resultado.get(0).getId());
        assertEquals("Ruta del Café", resultado.get(0).getNombre());
        assertEquals(java.time.LocalDate.of(2026, 11, 18), resultado.get(0).getFechaInicio());
        assertEquals(java.time.LocalDate.of(2026, 11, 19), resultado.get(0).getFechaFin());
        assertEquals(new BigDecimal("195000.00"), resultado.get(0).getPrecioBase());
        assertEquals(8, resultado.get(0).getCuposDisponibles());
    }

    @Test
    void listarItinerariosDisponibles_DestinoInexistenteLanzaNoEncontrado() {
        when(destinoRepository.findById(99L)).thenReturn(java.util.Optional.empty());

        assertThrows(com.sendera.exception.RecursoNoEncontradoException.class,
                () -> destinoService.listarItinerariosDisponibles(99L));
        verifyNoInteractions(itinerarioRepository);
    }

    @Test
    void eliminarDestino_DeberiaAuditarSoloSiSeElimina() {
        when(destinoRepository.findById(1L)).thenReturn(java.util.Optional.of(destinoEjemplo));
        when(itinerarioRepository.existsByDestinoId(1L)).thenReturn(false);
        com.sendera.security.UsuarioActual admin = new com.sendera.security.UsuarioActual(9L, true);

        destinoService.eliminarDestino(1L, admin);

        verify(destinoRepository).delete(destinoEjemplo);
        verify(auditoriaService).registrar(admin, "DESTINO_ELIMINAR", "destino", 1L, "Valle de Cocora");
    }

    @Test
    void eliminarDestinoConItinerarios_NoDeberiaAuditar() {
        when(destinoRepository.findById(1L)).thenReturn(java.util.Optional.of(destinoEjemplo));
        when(itinerarioRepository.existsByDestinoId(1L)).thenReturn(true);
        com.sendera.security.UsuarioActual admin = new com.sendera.security.UsuarioActual(9L, true);

        org.junit.jupiter.api.Assertions.assertThrows(com.sendera.exception.DestinoConItinerariosException.class,
                () -> destinoService.eliminarDestino(1L, admin));

        org.mockito.Mockito.verifyNoInteractions(auditoriaService);
    }
}
