package com.sendera.service;

import com.sendera.dto.CrearDestinoDto;
import com.sendera.dto.DestinoDto;
import com.sendera.dto.FiltroDestinoDto;
import com.sendera.dto.ItinerarioDto;
import com.sendera.dto.PaginaResultadosDto;
import com.sendera.security.UsuarioActual;

import java.util.List;

public interface DestinoService {
    /** Tamaño de página si el cliente no envía {@code size}. */
    int TAMANO_POR_DEFECTO = 12;
    /** Tope de {@code size}: un valor mayor se limita a este, no se rechaza. */
    int TAMANO_MAXIMO = 50;

    /**
     * Destinos que cumplen el filtro, paginados en la consulta y ordenados por id.
     *
     * @param pagina página a devolver, empezando en 0 (400 si es negativa)
     * @param tamano destinos por página (400 si es menor que 1; se limita a {@link #TAMANO_MAXIMO})
     */
    PaginaResultadosDto<DestinoDto> listarDestinos(FiltroDestinoDto filtro, int pagina, int tamano);
    DestinoDto obtenerDestinoPorId(Long id);
    /** Itinerarios reservables del destino (cupos > 0 y fecha de fin de hoy en adelante); 404 si el destino no existe. */
    List<ItinerarioDto> listarItinerariosDisponibles(Long destinoId);
    DestinoDto crearDestino(CrearDestinoDto dto, UsuarioActual actor);
    DestinoDto actualizarDestino(Long id, CrearDestinoDto dto, UsuarioActual actor);
    void eliminarDestino(Long id, UsuarioActual actor);
}