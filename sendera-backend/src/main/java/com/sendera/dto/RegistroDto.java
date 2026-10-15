package com.sendera.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegistroDto {

    @NotBlank(message = "Ingresa tu nombre.")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres.")
    private String nombre;

    @NotBlank(message = "Ingresa tus apellidos.")
    @Size(max = 120, message = "Los apellidos no pueden superar los 120 caracteres.")
    private String apellidos;

    @NotBlank(message = "Ingresa tu correo electrónico.")
    @Email(message = "Ingresa un correo válido.")
    @Size(max = 160, message = "El correo no puede superar los 160 caracteres.")
    private String correo;

    @NotBlank(message = "Ingresa una contraseña.")
    @Size(min = 8, max = 64, message = "La contraseña debe tener entre 8 y 64 caracteres.")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Usa letras y números en tu contraseña.")
    private String contrasena;
}
