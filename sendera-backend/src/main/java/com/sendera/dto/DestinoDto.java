package com.sendera.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class DestinoDto {
    private Long id;
    private String nombre;
    private String descripcion;
    private String ubicacion;
    private BigDecimal precioBase;
    private List<String> fotosUrl;
}