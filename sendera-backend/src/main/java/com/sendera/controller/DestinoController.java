package com.sendera.controller;

import com.sendera.dto.CrearDestinoDto;
import com.sendera.dto.DestinoDto;
import com.sendera.service.DestinoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/destinos")
@RequiredArgsConstructor
public class DestinoController {

    private final DestinoService destinoService;

    @GetMapping
    public ResponseEntity<List<DestinoDto>> listarDestinos() {
        return ResponseEntity.ok(destinoService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DestinoDto> obtenerDestinoPorId(@PathVariable Long id) {
        return ResponseEntity.ok(destinoService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<DestinoDto> crearDestino(@RequestBody CrearDestinoDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(destinoService.crear(dto));
    }
}