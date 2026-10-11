package com.sendera.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FiltroDestinoDto {

    private String q;
    private BigDecimal precioMin;
    private BigDecimal precioMax;
    private Integer duracionMin;
    private Integer duracionMax;
}
