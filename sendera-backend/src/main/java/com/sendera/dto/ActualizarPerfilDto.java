package com.sendera.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Datos de la cuenta que la persona puede cambiar. El rol nunca se modifica desde aquí (RF-004). */
@Data
public class ActualizarPerfilDto {

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

    /** Obligatoria solo si el correo cambia: evita que una sesión abierta ajena se apropie de la cuenta. */
    @Size(max = 64, message = "La contraseña no puede superar los 64 caracteres.")
    private String contrasenaActual;
}
