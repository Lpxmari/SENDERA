package com.sendera.service;

import com.sendera.dto.CrearDestinoDto;
import com.sendera.dto.DestinoDto;
import com.sendera.dto.FiltroDestinoDto;
import com.sendera.dto.ItinerarioDto;
import com.sendera.security.UsuarioActual;

import java.util.List;

public interface DestinoService {
    List<DestinoDto> listarDestinos(FiltroDestinoDto filtro);
    DestinoDto obtenerDestinoPorId(Long id);
    /** Itinerarios reservables del destino (cupos > 0 y fecha de fin de hoy en adelante); 404 si el destino no existe. */
    List<ItinerarioDto> listarItinerariosDisponibles(Long destinoId);
    DestinoDto crearDestino(CrearDestinoDto dto, UsuarioActual actor);
    DestinoDto actualizarDestino(Long id, CrearDestinoDto dto, UsuarioActual actor);
    void eliminarDestino(Long id, UsuarioActual actor);
}