package com.sendera.integration;

import java.math.BigDecimal;

/**
 * Datos de negocio para crear una preferencia de Checkout Pro. El formato JSON de Mercado Pago queda encapsulado
 * en {@link MercadoPagoClient}.
 *
 * @param urlRetorno      página del frontend a la que vuelve el comprador (éxito, pendiente o fallo)
 * @param autoRetorno     redirigir solo al aprobarse; Mercado Pago no lo acepta con URLs locales
 * @param urlNotificacion URL HTTPS pública del webhook; vacía para usar la configurada en "Tus integraciones"
 */
public record SolicitudPreferencia(String itemId, String titulo, BigDecimal monto, String moneda,
                                   String referenciaExterna, String urlRetorno, boolean autoRetorno,
                                   String urlNotificacion) {
}
