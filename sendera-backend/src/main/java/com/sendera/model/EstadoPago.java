package com.sendera.model;

/** Estado del pago de una reserva en la pasarela externa; es independiente del estado de la reserva. */
public enum EstadoPago {
    PENDIENTE,
    APROBADO,
    RECHAZADO,
    REEMBOLSADO
}
