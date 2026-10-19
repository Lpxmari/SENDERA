package com.sendera.exception;

public class TokenRecuperacionInvalidoException extends RuntimeException {
    public TokenRecuperacionInvalidoException() {
        super("El código no es correcto o ya venció. Revisa el último correo que te enviamos o solicita un código nuevo.");
    }

    public TokenRecuperacionInvalidoException(String mensaje) {
        super(mensaje);
    }
}
