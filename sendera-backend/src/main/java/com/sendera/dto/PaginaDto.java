package com.sendera.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Página de resultados de un listado administrativo. {@code pagina} empieza en 0. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginaDto<T> {
    private List<T> contenido;
    private int pagina;
    private int tamano;
    private long total;
}
