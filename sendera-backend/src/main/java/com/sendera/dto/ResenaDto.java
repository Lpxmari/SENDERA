package com.sendera.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ResenaDto {
    private Long id;
    private Long destinoId;
    private String comentario;
    private Integer calificacion;
    private String autor;
    private LocalDateTime creadaEn;
    private String imagenUrl;
}
