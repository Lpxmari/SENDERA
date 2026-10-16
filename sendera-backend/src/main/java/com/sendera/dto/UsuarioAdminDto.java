package com.sendera.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UsuarioAdminDto {
    private Long id;
    private String nombre;
    private String apellidos;
    private String correo;
    private String rol;
    private boolean activo;
    private LocalDateTime fechaRegistro;
}
