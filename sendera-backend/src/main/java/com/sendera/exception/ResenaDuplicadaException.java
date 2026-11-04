package com.sendera.exception;

/** La persona ya publicó una reseña de ese destino. Responde 409. */
public class ResenaDuplicadaException extends RuntimeException {

    public ResenaDuplicadaException() {
        super("Ya publicaste una reseña de este destino.");
    }
}
