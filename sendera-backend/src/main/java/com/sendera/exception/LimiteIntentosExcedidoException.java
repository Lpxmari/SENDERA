package com.sendera.exception;

/** Se superó el límite de intentos de inicio de sesión; el cliente debe esperar {@code segundosEspera}. */
public class LimiteIntentosExcedidoException extends RuntimeException {

    private final long segundosEspera;

    public LimiteIntentosExcedidoException(long segundosEspera) {
        super("Demasiados intentos de inicio de sesión. Espera un minuto e inténtalo de nuevo.");
        this.segundosEspera = segundosEspera;
    }

    public long getSegundosEspera() {
        return segundosEspera;
    }
}
