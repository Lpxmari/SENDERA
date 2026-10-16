package com.sendera.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CrearResenaDto {

    @NotBlank(message = "Escribe un comentario para publicar la reseña.")
    @Size(max = 2000, message = "El comentario no puede superar los 2000 caracteres.")
    private String comentario;

    @NotNull(message = "Selecciona una calificación de 1 a 5 estrellas.")
    @Min(value = 1, message = "La calificación debe ser de al menos 1 estrella.")
    @Max(value = 5, message = "La calificación no puede superar 5 estrellas.")
    private Integer calificacion;
}
