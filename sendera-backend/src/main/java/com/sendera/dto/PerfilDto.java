package com.sendera.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * Perfil completo de la cuenta propia (GET /auth/me). Los datos personales no viajan en {@link UsuarioDto},
 * que es lo que se guarda como sesión en el navegador.
 */
@Data
@Builder
public class PerfilDto {
    private Long id;
    private String nombre;
    private String apellidos;
    private String correo;
    private String rol;
    private String numeroIdentificacion;
    private LocalDate fechaNacimiento;
    private String paisResidencia;
    private List<String> destinosFavoritos;
    private List<String> intereses;
}
