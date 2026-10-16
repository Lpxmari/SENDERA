package com.sendera.controller;

import com.sendera.dto.CrearResenaDto;
import com.sendera.dto.ResenaDto;
import com.sendera.model.Resena;
import com.sendera.security.UsuarioActual;
import com.sendera.service.ResenaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/destinos/{destinoId}/resenas")
@RequiredArgsConstructor
public class ResenaController {

    private final ResenaService resenaService;

    @GetMapping
    public ResponseEntity<List<ResenaDto>> listar(@PathVariable Long destinoId) {
        return ResponseEntity.ok(resenaService.listarPorDestino(destinoId));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResenaDto> crear(@PathVariable Long destinoId,
                                           @Valid @ModelAttribute CrearResenaDto dto,
                                           @RequestParam(value = "imagen", required = false) MultipartFile imagen,
                                           @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(resenaService.crear(destinoId, dto, imagen, UsuarioActual.desde(jwt)));
    }

    @GetMapping("/{resenaId}/imagen")
    public ResponseEntity<byte[]> obtenerImagen(@PathVariable Long destinoId, @PathVariable Long resenaId) {
        Resena resena = resenaService.obtenerImagen(destinoId, resenaId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(resena.getImagenContentType()))
                .contentLength(resena.getImagen().length)
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"resena-" + resenaId + "\"")
                .body(resena.getImagen());
    }
}
