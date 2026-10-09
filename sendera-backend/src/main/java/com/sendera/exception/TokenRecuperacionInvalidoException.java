package com.sendera.exception;

public class TokenRecuperacionInvalidoException extends RuntimeException {
    public TokenRecuperacionInvalidoException() {
        super("El enlace no es válido o ya expiró. Solicita uno nuevo.");
    }
}
