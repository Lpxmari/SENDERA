package com.sendera.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class ReservaDto {
    private Long id;
    private Long itinerarioId;
    private LocalDate fechaReserva;
    private BigDecimal montoTotal;
    private String estadoReserva;
    /** Estado del pago en Mercado Pago; null si todavía no se ha iniciado un pago. */
    private String estadoPago;
    private Long destinoId;
    private String destinoNombre;
    private String itinerarioNombre;
}
