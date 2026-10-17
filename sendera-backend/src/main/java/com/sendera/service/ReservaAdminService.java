package com.sendera.service;

import com.sendera.dto.CambiarEstadoReservaDto;
import com.sendera.dto.PaginaDto;
import com.sendera.dto.ReservaAdminDto;
import com.sendera.security.UsuarioActual;

import java.time.LocalDate;

/** RF-012/RF-013: gestión administrativa de reservas. Los cambios de estado quedan auditados. */
public interface ReservaAdminService {
    PaginaDto<ReservaAdminDto> listar(String estado, String q, LocalDate desde, LocalDate hasta, int pagina, int tamano);
    ReservaAdminDto obtener(Long id);
    ReservaAdminDto cambiarEstado(Long id, CambiarEstadoReservaDto dto, UsuarioActual actor);
}
