package com.sendera.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/** Cuerpo para crear (POST) o reemplazar (PUT) un destino. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrearDestinoDto {

    @NotBlank(message = "El nombre del destino es obligatorio.")
    @Size(max = 255, message = "El nombre no puede superar los 255 caracteres.")
    private String nombre;

    @NotBlank(message = "La descripción del destino es obligatoria.")
    private String descripcion;

    @NotBlank(message = "La ubicación del destino es obligatoria.")
    @Size(max = 255, message = "La ubicación no puede superar los 255 caracteres.")
    private String ubicacion;

    @NotNull(message = "El precio base es obligatorio.")
    @DecimalMin(value = "0.0", message = "El precio base no puede ser negativo.")
    @Digits(integer = 10, fraction = 2, message = "El precio base admite hasta 10 enteros y 2 decimales.")
    private BigDecimal precioBase;

    // --- AÑADIDO PARA EL PROCESO 2 ---
    @NotBlank(message = "La dificultad del destino es obligatoria.")
    @Size(max = 20, message = "La dificultad admite hasta 20 caracteres.")
    private String dificultad;

    @NotNull(message = "La accesibilidad es obligatoria.")
    private Boolean accesibilidad;

    private List<@NotBlank(message = "Las URL de las fotos no pueden estar vacías.")
    @Size(max = 255, message = "Cada URL de foto admite hasta 255 caracteres.") String> fotosUrl;
}