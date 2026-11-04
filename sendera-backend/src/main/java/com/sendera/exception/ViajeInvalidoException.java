package com.sendera.exception;

/** El viaje incumple una regla de negocio (fechas pasadas, actividad fuera del rango, etc.): 400. */
public class ViajeInvalidoException extends RuntimeException {

    public ViajeInvalidoException(String mensaje) {
        super(mensaje);
    }
}
