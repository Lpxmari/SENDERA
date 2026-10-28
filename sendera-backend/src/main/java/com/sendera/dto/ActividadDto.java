package com.sendera.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ActividadDto {
    private Long id;
    private Long destinoId;
    private String destinoNombre;
    /** Ubicación del destino al que pertenece la actividad. */
    private String ubicacion;
    private String nombre;
    private String descripcion;
    private Integer duracionMinutos;
    private String dificultad;
    private BigDecimal precioPorPersona;
    private Boolean accesibilidad;
}
