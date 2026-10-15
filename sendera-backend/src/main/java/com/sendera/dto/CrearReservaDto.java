package com.sendera.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/** El monto y el estado los define el servidor; cualquier valor enviado por el cliente se ignora. */
@Data
public class CrearReservaDto {

    @NotNull(message = "Selecciona un itinerario para reservar.")
    private Long itinerarioId;

    private LocalDate fechaReserva;
}
