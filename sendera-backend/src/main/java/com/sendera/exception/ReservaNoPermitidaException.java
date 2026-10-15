package com.sendera.exception;

/** La reserva no puede crearse o cambiar de estado por el estado actual del itinerario o de la reserva. */
public class ReservaNoPermitidaException extends RuntimeException {

    public ReservaNoPermitidaException(String mensaje) {
        super(mensaje);
    }
}
