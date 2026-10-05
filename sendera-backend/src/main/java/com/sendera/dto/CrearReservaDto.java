package com.sendera.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CrearReservaDto {
    private Long itinerarioId;
    private LocalDate fechaReserva;
    private BigDecimal montoTotal;
    private String estadoPago;
}