package com.sendera.service;

import com.sendera.exception.FiltroInvalidoException;
import com.sendera.repository.ActividadRepository;
import com.sendera.service.impl.ActividadServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** RNF-26: la duración se traduce a un rango de minutos antes de consultar el repositorio. */
class ActividadServiceImplTest {

    private final ActividadRepository repository = mock(ActividadRepository.class);
    private final ActividadServiceImpl service = new ActividadServiceImpl(repository);

    @Test
    void sinDuracionNoAplicaLimites() {
        when(repository.buscar(null, null, null)).thenReturn(List.of());

        service.listar(null, null);
        service.listar(null, "  ");

        verify(repository, org.mockito.Mockito.times(2)).buscar(null, null, null);
    }

    @Test
    void traduceCadaRangoAMinutos() {
        when(repository.buscar(any(), any(), any())).thenReturn(List.of());

        service.listar(1L, "HASTA_60_MIN");
        service.listar(1L, "DE_61_A_180_MIN");
        service.listar(null, "MAS_DE_180_MIN");

        verify(repository).buscar(1L, 1, 60);
        verify(repository).buscar(1L, 61, 180);
        verify(repository).buscar(null, 181, Integer.MAX_VALUE);
    }

    @Test
    void aceptaMinusculasYEspacios() {
        when(repository.buscar(any(), any(), any())).thenReturn(List.of());

        service.listar(null, " de_61_a_180_min ");

        verify(repository).buscar(null, 61, 180);
    }

    @Test
    void unValorDesconocidoSeRechazaSinConsultar() {
        FiltroInvalidoException ex = assertThrows(FiltroInvalidoException.class, () -> service.listar(null, "RAPIDA"));

        assertEquals("La duración debe ser HASTA_60_MIN, DE_61_A_180_MIN o MAS_DE_180_MIN.", ex.getMessage());
        verify(repository, never()).buscar(any(), any(), any());
    }
}
