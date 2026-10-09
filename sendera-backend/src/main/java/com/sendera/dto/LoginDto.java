package com.sendera.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginDto {

    @NotBlank(message = "Ingresa tu correo electrónico.")
    private String correo;

    @NotBlank(message = "Ingresa tu contraseña.")
    private String contrasena;
}
