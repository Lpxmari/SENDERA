package com.sendera.service;

import com.sendera.dto.ActividadDto;

import java.util.List;

/** Catálogo de actividades (solo lectura; la gestión administrativa queda fuera de esta entrega). */
public interface ActividadService {
    List<ActividadDto> listar(Long destinoId);
    ActividadDto obtenerPorId(Long id);
}
