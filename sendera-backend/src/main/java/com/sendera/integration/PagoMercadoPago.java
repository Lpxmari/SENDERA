package com.sendera.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/** Pago consultado en GET /v1/payments/{id}; es la única fuente confiable del estado (nunca el cuerpo del webhook). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PagoMercadoPago(
        Long id,
        String status,
        @JsonProperty("external_reference") String externalReference,
        @JsonProperty("transaction_amount") BigDecimal transactionAmount,
        @JsonProperty("currency_id") String currencyId) {
}
