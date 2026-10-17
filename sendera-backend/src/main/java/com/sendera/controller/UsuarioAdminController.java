package com.sendera.controller;

import com.sendera.dto.CambiarActivoDto;
import com.sendera.dto.CambiarRolDto;
import com.sendera.dto.PaginaDto;
import com.sendera.dto.UsuarioAdminDto;
import com.sendera.security.UsuarioActual;
import com.sendera.service.UsuarioAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/** RF-013. Solo ROLE_ADMIN: se exige en SecurityConfig para todo {@code /admin/**}. */
@RestController
@RequestMapping("/admin/usuarios")
@RequiredArgsConstructor
public class UsuarioAdminController {

    private final UsuarioAdminService usuarioAdminService;

    @GetMapping
    public ResponseEntity<PaginaDto<UsuarioAdminDto>> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String rol,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        return ResponseEntity.ok(usuarioAdminService.listar(q, rol, activo, pagina, tamano));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioAdminDto> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioAdminService.obtener(id));
    }

    @PatchMapping("/{id}/rol")
    public ResponseEntity<UsuarioAdminDto> cambiarRol(@PathVariable Long id, @Valid @RequestBody CambiarRolDto dto,
                                                      @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(usuarioAdminService.cambiarRol(id, dto, UsuarioActual.desde(jwt)));
    }

    @PatchMapping("/{id}/activo")
    public ResponseEntity<UsuarioAdminDto> cambiarActivo(@PathVariable Long id, @Valid @RequestBody CambiarActivoDto dto,
                                                         @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(usuarioAdminService.cambiarActivo(id, dto, UsuarioActual.desde(jwt)));
    }
}
