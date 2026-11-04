package com.sendera.service;

import com.sendera.dto.ActividadDto;

import java.util.List;

/** Catálogo de actividades (solo lectura; la gestión administrativa queda fuera de esta entrega). */
public interface ActividadService {
    /** @param duracion HASTA_60_MIN, DE_61_A_180_MIN o MAS_DE_180_MIN (sin distinguir mayúsculas); null = todas. */
    List<ActividadDto> listar(Long destinoId, String duracion);
    ActividadDto obtenerPorId(Long id);
}
