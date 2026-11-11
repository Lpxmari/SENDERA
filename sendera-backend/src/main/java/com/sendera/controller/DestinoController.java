package com.sendera.controller;

import com.sendera.dto.CrearDestinoDto;
import com.sendera.dto.DestinoDto;
import com.sendera.dto.FiltroDestinoDto;
import com.sendera.dto.ItinerarioDto;
import com.sendera.dto.PaginaResultadosDto;
import com.sendera.service.DestinoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.sendera.security.UsuarioActual;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/destinos")
@RequiredArgsConstructor
public class DestinoController {

    private final DestinoService destinoService;

    /**
     * Lista destinos paginados; todos los filtros son opcionales y se combinan con AND.
     * {@code page} empieza en 0 y {@code size} se limita a {@link DestinoService#TAMANO_MAXIMO}.
     */
    @GetMapping
    public ResponseEntity<PaginaResultadosDto<DestinoDto>> listarDestinos(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String ubicacion,
            @RequestParam(required = false) BigDecimal precioMin,
            @RequestParam(required = false) BigDecimal precioMax,
            @RequestParam(required = false) String dificultad,
            @RequestParam(required = false) Boolean accesibilidad,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + DestinoService.TAMANO_POR_DEFECTO) int size) {
        FiltroDestinoDto filtro = FiltroDestinoDto.builder()
                .q(q)
                .nombre(nombre)
                .ubicacion(ubicacion)
                .precioMin(precioMin)
                .precioMax(precioMax)
                .dificultad(dificultad)
                .accesibilidad(accesibilidad)
                .build();
        return ResponseEntity.ok(destinoService.listarDestinos(filtro, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DestinoDto> obtenerDestinoPorId(@PathVariable Long id) {
        return ResponseEntity.ok(destinoService.obtenerDestinoPorId(id));
    }

    @GetMapping("/{id}/itinerarios")
    public ResponseEntity<List<ItinerarioDto>> listarItinerariosDeDestino(@PathVariable Long id) {
        return ResponseEntity.ok(destinoService.listarItinerariosDisponibles(id));
    }

    @PostMapping
    public ResponseEntity<DestinoDto> crearDestino(@Valid @RequestBody CrearDestinoDto dto,
                                                   @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(destinoService.crearDestino(dto, UsuarioActual.desde(jwt)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DestinoDto> actualizarDestino(@PathVariable Long id,
                                                        @Valid @RequestBody CrearDestinoDto dto,
                                                        @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(destinoService.actualizarDestino(id, dto, UsuarioActual.desde(jwt)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarDestino(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        destinoService.eliminarDestino(id, UsuarioActual.desde(jwt));
        return ResponseEntity.noContent().build();
    }
}