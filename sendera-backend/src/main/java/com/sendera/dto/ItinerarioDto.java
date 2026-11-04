package com.sendera.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Itinerario reservable de un destino; su id es el itinerarioId que recibe POST /reservas. */
@Data
@Builder
public class ItinerarioDto {
    private Long id;
    private String nombre;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private BigDecimal precioBase;
    private Integer cuposDisponibles;
}
