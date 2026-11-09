package com.sendera.dto;

/**
 * Cuerpo de un webhook de Mercado Pago, p. ej. {@code {"type":"payment","action":"payment.updated","data":{"id":"123"}}}.
 * Solo indica qué pago cambió: su estado real siempre se consulta a la API de Mercado Pago.
 */
public record NotificacionMercadoPagoDto(String type, String action, Datos data) {

    public record Datos(String id) {
    }
}
