package com.sendera.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
public class ViajeActividadDto {
    private Long id;
    private Long actividadId;
    private Long destinoId;
    private String nombre;
    private String ubicacion;
    private LocalDate fecha;
    @JsonFormat(pattern = "HH:mm")
    private LocalTime horaInicio;
    private Integer duracionMinutos;
    private String dificultad;
    /** Posición dentro del día, desde 1. */
    private Integer orden;
}
