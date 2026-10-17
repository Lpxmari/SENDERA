package com.sendera.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** Estado al que el administrador lleva la reserva. PENDIENTE no es un destino válido. */
@Data
public class CambiarEstadoReservaDto {

    @NotBlank(message = "El estado es obligatorio.")
    @Pattern(regexp = "CONFIRMADA|CANCELADA", message = "El estado debe ser CONFIRMADA o CANCELADA.")
    private String estado;
}
