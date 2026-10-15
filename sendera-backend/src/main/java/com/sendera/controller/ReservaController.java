package com.sendera.controller;

import com.sendera.dto.CrearReservaDto;
import com.sendera.dto.ReservaDto;
import com.sendera.security.UsuarioActual;
import com.sendera.service.ReservaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService reservaService;

    @GetMapping
    public ResponseEntity<List<ReservaDto>> listarReservas(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(reservaService.listar(UsuarioActual.desde(jwt)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservaDto> obtenerReservaPorId(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(reservaService.obtenerPorId(id, UsuarioActual.desde(jwt)));
    }

    @PostMapping
    public ResponseEntity<ReservaDto> crearReserva(@Valid @RequestBody CrearReservaDto dto,
                                                   @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservaService.crear(dto, UsuarioActual.desde(jwt)));
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<ReservaDto> cancelarReserva(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(reservaService.cancelar(id, UsuarioActual.desde(jwt)));
    }
}
