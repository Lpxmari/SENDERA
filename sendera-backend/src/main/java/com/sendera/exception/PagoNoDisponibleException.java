package com.sendera.exception;

/** El pago en línea no está configurado en este entorno (falta MERCADOPAGO_ACCESS_TOKEN). */
public class PagoNoDisponibleException extends RuntimeException {

    public PagoNoDisponibleException() {
        super("El pago en línea no está disponible por ahora. Intenta más tarde.");
    }
}
