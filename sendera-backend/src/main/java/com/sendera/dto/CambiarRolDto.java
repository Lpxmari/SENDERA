package com.sendera.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CambiarRolDto {

    @NotBlank(message = "El rol es obligatorio.")
    @Pattern(regexp = "ROLE_ADMIN|ROLE_CLIENTE", message = "El rol debe ser ROLE_ADMIN o ROLE_CLIENTE.")
    private String rol;
}
