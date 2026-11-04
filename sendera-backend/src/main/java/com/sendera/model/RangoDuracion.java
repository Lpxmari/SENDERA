package com.sendera.model;

import com.sendera.exception.FiltroInvalidoException;

/** Rangos de duración de una actividad, en minutos y con límites inclusivos (RNF-26, RF-008). */
public enum RangoDuracion {
    HASTA_60_MIN(1, 60),
    DE_61_A_180_MIN(61, 180),
    MAS_DE_180_MIN(181, Integer.MAX_VALUE);

    private final int minMinutos;
    private final int maxMinutos;

    RangoDuracion(int minMinutos, int maxMinutos) {
        this.minMinutos = minMinutos;
        this.maxMinutos = maxMinutos;
    }

    public int getMinMinutos() {
        return minMinutos;
    }

    public int getMaxMinutos() {
        return maxMinutos;
    }

    /** Interpreta el parámetro de la API sin distinguir mayúsculas; vacío o nulo significa «sin filtro». */
    public static RangoDuracion desdeTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new FiltroInvalidoException(
                    "La duración debe ser HASTA_60_MIN, DE_61_A_180_MIN o MAS_DE_180_MIN.");
        }
    }
}
