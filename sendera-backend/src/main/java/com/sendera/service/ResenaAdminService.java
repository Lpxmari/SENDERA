package com.sendera.service;

import com.sendera.dto.PaginaDto;
import com.sendera.dto.ResenaAdminDto;
import com.sendera.security.UsuarioActual;

/** RF-013: moderación de reseñas. Eliminar una reseña queda auditado. */
public interface ResenaAdminService {
    PaginaDto<ResenaAdminDto> listar(Long destinoId, Integer calificacionMax, String q, int pagina, int tamano);
    void eliminar(Long id, UsuarioActual actor);
}
