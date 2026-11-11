package com.sendera.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * Datos de la cuenta que la persona puede cambiar. El rol nunca se modifica desde aquí (RF-004).
 * Los campos del perfil del viajero (NC-01) son opcionales: si no se envían (null) se conservan los guardados;
 * una lista vacía borra la lista.
 */
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

    @Pattern(regexp = "^\\s*\\d{5,15}\\s*$", message = "El número de identificación debe tener entre 5 y 15 dígitos.")
    private String numeroIdentificacion;

    @PastOrPresent(message = "La fecha de nacimiento no puede ser futura.")
    private LocalDate fechaNacimiento;

    @Pattern(regexp = "^.*\\S.*$", message = "Indica tu país de residencia.")
    @Size(max = 80, message = "El país no puede superar los 80 caracteres.")
    private String paisResidencia;

    @Size(max = 10, message = "Puedes guardar hasta 10 destinos favoritos.")
    private List<@NotBlank(message = "Los destinos favoritos no pueden estar vacíos.")
            @Size(max = 80, message = "Cada destino favorito puede tener hasta 80 caracteres.") String> destinosFavoritos;

    @Size(max = 10, message = "Puedes guardar hasta 10 intereses.")
    private List<@NotBlank(message = "Los intereses no pueden estar vacíos.")
            @Size(max = 60, message = "Cada interés puede tener hasta 60 caracteres.") String> intereses;
}
