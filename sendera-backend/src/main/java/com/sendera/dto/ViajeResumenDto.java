package com.sendera.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
public class ViajeResumenDto {
    private Long id;
    private String nombre;
    /** Ubicación del destino de la primera actividad; nula si el viaje aún no tiene actividades. */
    private String ubicacion;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private int totalActividades;
    /** Hora de inicio de la primera actividad; nula si el viaje aún no tiene actividades. */
    @JsonFormat(pattern = "HH:mm")
    private LocalTime primeraHora;
}
