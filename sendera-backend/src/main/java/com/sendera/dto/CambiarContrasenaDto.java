package com.sendera.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CambiarContrasenaDto {

    @NotBlank(message = "El enlace no es válido. Solicita uno nuevo.")
    private String token;

    @NotBlank(message = "Ingresa una contraseña.")
    @Size(min = 8, max = 64, message = "La contraseña debe tener entre 8 y 64 caracteres.")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Usa letras y números en tu contraseña.")
    private String nuevaContrasena;
}
