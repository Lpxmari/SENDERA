package com.sendera.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrearDestinoDto {

    private String nombre;
    private String descripcion;
    private String ubicacion;
    private BigDecimal precioBase;
    private List<String> fotosUrl;
}