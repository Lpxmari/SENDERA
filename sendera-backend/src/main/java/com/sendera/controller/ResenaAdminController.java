package com.sendera.controller;

import com.sendera.dto.PaginaDto;
import com.sendera.dto.ResenaAdminDto;
import com.sendera.security.UsuarioActual;
import com.sendera.service.ResenaAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/** RF-013. Solo ROLE_ADMIN: se exige en SecurityConfig para todo {@code /admin/**}. */
@RestController
@RequestMapping("/admin/resenas")
@RequiredArgsConstructor
public class ResenaAdminController {

    private final ResenaAdminService resenaAdminService;

    @GetMapping
    public ResponseEntity<PaginaDto<ResenaAdminDto>> listar(
            @RequestParam(required = false) Long destinoId,
            @RequestParam(required = false) Integer calificacionMax,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        return ResponseEntity.ok(resenaAdminService.listar(destinoId, calificacionMax, q, pagina, tamano));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        resenaAdminService.eliminar(id, UsuarioActual.desde(jwt));
        return ResponseEntity.noContent().build();
    }
}
