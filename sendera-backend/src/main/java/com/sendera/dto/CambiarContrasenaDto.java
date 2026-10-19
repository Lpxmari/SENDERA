package com.sendera.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CambiarContrasenaDto {

    @NotBlank(message = "Ingresa tu correo electrónico.")
    @Email(message = "Ingresa un correo válido.")
    private String correo;

    @NotBlank(message = "El código tiene 6 números.")
    @Pattern(regexp = "^\\d{6}$", message = "El código tiene 6 números.")
    private String codigo;

    @NotBlank(message = "Ingresa una contraseña.")
    @Size(min = 8, max = 64, message = "La contraseña debe tener entre 8 y 64 caracteres.")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Usa letras y números en tu contraseña.")
    private String nuevaContrasena;
}
