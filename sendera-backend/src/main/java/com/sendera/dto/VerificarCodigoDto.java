package com.sendera.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VerificarCodigoDto {

    @NotBlank(message = "Ingresa tu correo electrónico.")
    @Email(message = "Ingresa un correo válido.")
    private String correo;

    @NotBlank(message = "El código tiene 6 números.")
    @Pattern(regexp = "^\\d{6}$", message = "El código tiene 6 números.")
    private String codigo;
}
