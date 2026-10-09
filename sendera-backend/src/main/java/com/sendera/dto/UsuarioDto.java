package com.sendera.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UsuarioDto {
    private Long id;
    private String nombre;
    private String apellidos;
    private String correo;
}
