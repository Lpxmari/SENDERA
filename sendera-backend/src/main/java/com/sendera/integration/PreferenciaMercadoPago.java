package com.sendera.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Respuesta de POST /checkout/preferences (solo los campos que usa SENDERA). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PreferenciaMercadoPago(
        String id,
        @JsonProperty("init_point") String initPoint,
        @JsonProperty("sandbox_init_point") String sandboxInitPoint) {
}
