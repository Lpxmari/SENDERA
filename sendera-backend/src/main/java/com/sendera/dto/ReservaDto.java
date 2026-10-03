package com.sendera.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservaDto {

    private Long id;
    private Long destinoId;
    private String nombreDestino;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private Integer cantidadPersonas;
    private BigDecimal precioTotal;
    private String estado;
}