package com.sendera.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RecuperarContrasenaDto {

    @NotBlank(message = "Ingresa tu correo electrónico.")
    @Email(message = "Ingresa un correo válido.")
    private String correo;
}
