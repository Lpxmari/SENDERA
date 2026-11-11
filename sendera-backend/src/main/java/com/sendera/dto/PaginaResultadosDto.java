package com.sendera.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Página de resultados de un listado público (p. ej. GET /destinos). {@code pagina} empieza en 0.
 * Es propio de la API: no se devuelve el {@code Page} de Spring.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginaResultadosDto<T> {
    private List<T> contenido;
    private int pagina;
    private int tamano;
    private long totalElementos;
    private int totalPaginas;
}
