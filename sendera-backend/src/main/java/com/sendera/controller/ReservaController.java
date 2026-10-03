package com.sendera.controller;

import com.sendera.dto.CrearReservaDto;
import com.sendera.dto.ReservaDto;
import com.sendera.service.ReservaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService reservaService;

    @GetMapping
    public ResponseEntity<List<ReservaDto>> listarReservas() {
        return ResponseEntity.ok(reservaService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservaDto> obtenerReservaPorId(@PathVariable Long id) {
        return ResponseEntity.ok(reservaService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<ReservaDto> crearReserva(@Valid @RequestBody CrearReservaDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservaService.crear(dto));
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<ReservaDto> cancelarReserva(@PathVariable Long id) {
        return ResponseEntity.ok(reservaService.cancelar(id));
    }

}