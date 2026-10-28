package com.sendera.controller;

import com.sendera.dto.ActividadDto;
import com.sendera.service.ActividadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/actividades")
@RequiredArgsConstructor
public class ActividadController {

    private final ActividadService actividadService;

    @GetMapping
    public ResponseEntity<List<ActividadDto>> listarActividades(@RequestParam(required = false) Long destinoId) {
        return ResponseEntity.ok(actividadService.listar(destinoId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ActividadDto> obtenerActividadPorId(@PathVariable Long id) {
        return ResponseEntity.ok(actividadService.obtenerPorId(id));
    }
}
