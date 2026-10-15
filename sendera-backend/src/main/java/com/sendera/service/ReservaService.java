package com.sendera.service;

import com.sendera.dto.CrearReservaDto;
import com.sendera.dto.ReservaDto;
import com.sendera.security.UsuarioActual;

import java.util.List;

/**
 * Las reservas pertenecen a un usuario: quien no es propietario ni administrador recibe 404,
 * para no revelar si la reserva existe.
 */
public interface ReservaService {
    List<ReservaDto> listar(UsuarioActual solicitante);
    ReservaDto obtenerPorId(Long id, UsuarioActual solicitante);
    ReservaDto crear(CrearReservaDto dto, UsuarioActual solicitante);
    ReservaDto cancelar(Long id, UsuarioActual solicitante);
}
