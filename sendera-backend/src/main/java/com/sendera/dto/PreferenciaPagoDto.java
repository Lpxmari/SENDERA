package com.sendera.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Preferencia de Checkout Pro creada para una reserva: el frontend redirige al comprador a {@code urlPago}. */
@Data
@AllArgsConstructor
public class PreferenciaPagoDto {
    private String preferenciaId;
    private String urlPago;
}
