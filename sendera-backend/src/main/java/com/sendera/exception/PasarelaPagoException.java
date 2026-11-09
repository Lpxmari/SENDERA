package com.sendera.exception;

/** Mercado Pago no respondió o rechazó la operación; el detalle técnico queda solo en el log. */
public class PasarelaPagoException extends RuntimeException {

    public PasarelaPagoException() {
        super("No pudimos comunicarnos con Mercado Pago. Intenta de nuevo en unos minutos.");
    }
}
