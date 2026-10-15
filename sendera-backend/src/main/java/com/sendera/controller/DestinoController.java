package com.sendera.controller;

import com.sendera.dto.CrearDestinoDto;
import com.sendera.dto.DestinoDto;
import com.sendera.dto.FiltroDestinoDto;
import com.sendera.service.DestinoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/destinos")
@RequiredArgsConstructor
public class DestinoController {

    private final DestinoService destinoService;

    /** Lista destinos; todos los parámetros son opcionales y se combinan con AND. */
    @GetMapping
    public ResponseEntity<List<DestinoDto>> listarDestinos(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) BigDecimal precioMin,
            @RequestParam(required = false) BigDecimal precioMax,
            @RequestParam(required = false) Integer duracionMin,
            @RequestParam(required = false) Integer duracionMax,
            @RequestParam(required = false) String dificultad,
            @RequestParam(required = false) Boolean accesibilidad) {
        FiltroDestinoDto filtro = FiltroDestinoDto.builder()
                .q(q)
                .precioMin(precioMin)
                .precioMax(precioMax)
                .duracionMin(duracionMin)
                .duracionMax(duracionMax)
                .dificultad(dificultad)
                .accesibilidad(accesibilidad)
                .build();
        return ResponseEntity.ok(destinoService.listarDestinos(filtro));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DestinoDto> obtenerDestinoPorId(@PathVariable Long id) {
        return ResponseEntity.ok(destinoService.obtenerDestinoPorId(id));
    }

    @PostMapping
    public ResponseEntity<DestinoDto> crearDestino(@Valid @RequestBody CrearDestinoDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(destinoService.crearDestino(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DestinoDto> actualizarDestino(@PathVariable Long id,
                                                        @Valid @RequestBody CrearDestinoDto dto) {
        return ResponseEntity.ok(destinoService.actualizarDestino(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarDestino(@PathVariable Long id) {
        destinoService.eliminarDestino(id);
        return ResponseEntity.noContent().build();
    }
}