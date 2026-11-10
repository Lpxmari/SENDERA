package com.sendera.exception;

/** La notificación no trae una firma x-signature válida para el secreto configurado. */
public class FirmaWebhookInvalidaException extends RuntimeException {

    public FirmaWebhookInvalidaException() {
        super("La notificación no tiene una firma válida.");
    }
}
