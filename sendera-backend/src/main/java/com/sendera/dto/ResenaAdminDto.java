package com.sendera.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** Reseña vista por un administrador para moderarla. No incluye la imagen, solo si existe. */
@Data
@Builder
public class ResenaAdminDto {
    private Long id;
    private Long destinoId;
    private String destinoNombre;
    private Long usuarioId;
    private String autor;
    private String comentario;
    private Integer calificacion;
    private boolean tieneImagen;
    private LocalDateTime creadaEn;
}
