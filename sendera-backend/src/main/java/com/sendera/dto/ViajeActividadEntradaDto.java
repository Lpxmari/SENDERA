package com.sendera.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/** Una actividad colocada en un día y hora. La posición dentro del día la asigna el servidor según la hora. */
@Data
public class ViajeActividadEntradaDto {

    @NotNull(message = "Selecciona una actividad.")
    private Long actividadId;

    @NotNull(message = "Indica el día de la actividad.")
    private LocalDate fecha;

    @NotNull(message = "Indica la hora de inicio de la actividad.")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime horaInicio;
}
