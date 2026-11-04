package com.sendera.exception;

/** La persona no cumple la condición para reseñar el destino (reserva confirmada). Responde 403. */
public class ResenaNoPermitidaException extends RuntimeException {

    public ResenaNoPermitidaException() {
        super("Solo puedes opinar sobre destinos que ya reservaste y cuya reserva fue confirmada.");
    }
}
