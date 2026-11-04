package com.sendera.controller;

import com.sendera.dto.GuardarViajeDto;
import com.sendera.dto.ViajeDto;
import com.sendera.dto.ViajeResumenDto;
import com.sendera.security.UsuarioActual;
import com.sendera.service.ViajeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/viajes")
@RequiredArgsConstructor
public class ViajeController {

    private final ViajeService viajeService;

    @GetMapping
    public ResponseEntity<List<ViajeResumenDto>> listarViajes(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(viajeService.listar(UsuarioActual.desde(jwt)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ViajeDto> obtenerViaje(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(viajeService.obtenerPorId(id, UsuarioActual.desde(jwt)));
    }

    @PostMapping
    public ResponseEntity<ViajeDto> crearViaje(@Valid @RequestBody GuardarViajeDto dto, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(viajeService.crear(dto, UsuarioActual.desde(jwt)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ViajeDto> actualizarViaje(@PathVariable Long id, @Valid @RequestBody GuardarViajeDto dto,
                                                    @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(viajeService.actualizar(id, dto, UsuarioActual.desde(jwt)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarViaje(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        viajeService.eliminar(id, UsuarioActual.desde(jwt));
        return ResponseEntity.noContent().build();
    }
}
