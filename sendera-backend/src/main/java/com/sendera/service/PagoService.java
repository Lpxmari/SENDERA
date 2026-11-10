package com.sendera.service;

import com.sendera.dto.PreferenciaPagoDto;
import com.sendera.dto.ReservaDto;
import com.sendera.security.UsuarioActual;

/**
 * Pago de reservas con Mercado Pago Checkout Pro (RF-014). El monto sale de la reserva y el estado del pago
 * siempre se consulta a la API de Mercado Pago; nunca se confía en datos enviados por el navegador.
 */
public interface PagoService {

    /** Crea una preferencia de pago para una reserva PENDIENTE del solicitante. */
    PreferenciaPagoDto iniciarPago(Long reservaId, UsuarioActual solicitante);

    /** Consulta en Mercado Pago los pagos de la reserva, actualiza su estado y devuelve la reserva. */
    ReservaDto verificarPago(Long reservaId, UsuarioActual solicitante);

    /**
     * Procesa un webhook ya autenticado de Mercado Pago. Solo atiende notificaciones de tipo "payment";
     * las demás, y los pagos que no pertenecen a una reserva de SENDERA, se ignoran.
     */
    void procesarNotificacion(String tipo, String pagoId);
}
