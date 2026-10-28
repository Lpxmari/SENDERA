package com.sendera.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Reemplaza el contenido completo del viaje. El propietario sale del JWT, nunca de este cuerpo. */
@Data
public class GuardarViajeDto {

    @NotBlank(message = "Escribe el nombre del viaje.")
    @Size(max = 120, message = "El nombre del viaje no puede superar los 120 caracteres.")
    private String nombre;

    @NotNull(message = "Indica la fecha de inicio.")
    private LocalDate fechaInicio;

    @NotNull(message = "Indica la fecha de finalización.")
    private LocalDate fechaFin;

    @Valid
    @Size(max = 50, message = "Un viaje no puede tener más de 50 actividades.")
    private List<ViajeActividadEntradaDto> actividades = new ArrayList<>();
}
