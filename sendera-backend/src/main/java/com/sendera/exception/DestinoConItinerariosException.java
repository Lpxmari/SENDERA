package com.sendera.exception;

public class DestinoConItinerariosException extends RuntimeException {
    public DestinoConItinerariosException() {
        super("No se puede retirar el destino porque tiene itinerarios asociados.");
    }
}
