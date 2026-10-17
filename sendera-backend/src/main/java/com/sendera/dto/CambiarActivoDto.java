package com.sendera.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CambiarActivoDto {

    @NotNull(message = "Indica si la cuenta debe quedar activa.")
    private Boolean activo;
}
