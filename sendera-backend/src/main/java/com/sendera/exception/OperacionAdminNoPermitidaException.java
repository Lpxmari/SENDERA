package com.sendera.exception;

/** La acción administrativa dejaría el sistema en un estado inválido (p. ej., sin administradores). */
public class OperacionAdminNoPermitidaException extends RuntimeException {

    public OperacionAdminNoPermitidaException(String mensaje) {
        super(mensaje);
    }
}
