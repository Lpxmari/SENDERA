package com.sendera.controller;

import com.sendera.dto.CambiarEstadoReservaDto;
import com.sendera.dto.PaginaDto;
import com.sendera.dto.ReservaAdminDto;
import com.sendera.security.UsuarioActual;
import com.sendera.service.ReservaAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/** RF-012/RF-013. Solo ROLE_ADMIN: se exige en SecurityConfig para todo {@code /admin/**}. */
@RestController
@RequestMapping("/admin/reservas")
@RequiredArgsConstructor
public class ReservaAdminController {

    private final ReservaAdminService reservaAdminService;

    @GetMapping
    public ResponseEntity<PaginaDto<ReservaAdminDto>> listar(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        return ResponseEntity.ok(reservaAdminService.listar(estado, q, desde, hasta, pagina, tamano));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservaAdminDto> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(reservaAdminService.obtener(id));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<ReservaAdminDto> cambiarEstado(@PathVariable Long id,
                                                         @Valid @RequestBody CambiarEstadoReservaDto dto,
                                                         @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(reservaAdminService.cambiarEstado(id, dto, UsuarioActual.desde(jwt)));
    }
}
