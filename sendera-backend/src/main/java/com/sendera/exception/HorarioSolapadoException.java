package com.sendera.exception;

/** Dos actividades del mismo día se solapan: el guardado se bloquea (409). */
public class HorarioSolapadoException extends RuntimeException {

    public HorarioSolapadoException(String mensaje) {
        super(mensaje);
    }
}
