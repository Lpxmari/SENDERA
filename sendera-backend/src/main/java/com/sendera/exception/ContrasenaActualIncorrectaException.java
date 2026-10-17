package com.sendera.exception;

/** La contraseña actual no coincide al pedir un cambio sensible, como el correo de la cuenta. */
public class ContrasenaActualIncorrectaException extends RuntimeException {

    public ContrasenaActualIncorrectaException() {
        super("Para cambiar tu correo escribe tu contraseña actual correctamente.");
    }
}
