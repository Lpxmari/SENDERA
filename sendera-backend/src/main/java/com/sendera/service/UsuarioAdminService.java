package com.sendera.service;

import com.sendera.dto.CambiarActivoDto;
import com.sendera.dto.CambiarRolDto;
import com.sendera.dto.PaginaDto;
import com.sendera.dto.UsuarioAdminDto;
import com.sendera.security.UsuarioActual;

/** RF-013: gestión administrativa de usuarios. Las modificaciones quedan auditadas. */
public interface UsuarioAdminService {
    PaginaDto<UsuarioAdminDto> listar(String q, String rol, Boolean activo, int pagina, int tamano);
    UsuarioAdminDto obtener(Long id);
    UsuarioAdminDto cambiarRol(Long id, CambiarRolDto dto, UsuarioActual actor);
    UsuarioAdminDto cambiarActivo(Long id, CambiarActivoDto dto, UsuarioActual actor);
}
