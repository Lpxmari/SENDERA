package com.sendera.service;

import com.sendera.dto.GuardarViajeDto;
import com.sendera.dto.ViajeDto;
import com.sendera.dto.ViajeResumenDto;
import com.sendera.security.UsuarioActual;

import java.util.List;

/**
 * Itinerarios personales (RF-009, RF-010). Cada viaje pertenece a un usuario: quien no es el propietario
 * recibe 404 (no se revela si existe), sin excepción para administradores.
 */
public interface ViajeService {
    List<ViajeResumenDto> listar(UsuarioActual solicitante);
    ViajeDto obtenerPorId(Long id, UsuarioActual solicitante);
    ViajeDto crear(GuardarViajeDto dto, UsuarioActual solicitante);
    ViajeDto actualizar(Long id, GuardarViajeDto dto, UsuarioActual solicitante);
    void eliminar(Long id, UsuarioActual solicitante);
}
