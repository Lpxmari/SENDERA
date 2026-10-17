package com.sendera.controller;

import com.sendera.dto.*;
import com.sendera.security.UsuarioActual;
import com.sendera.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/registro")
    public ResponseEntity<AuthRespuestaDto> registrar(@Valid @RequestBody RegistroDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(dto));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthRespuestaDto> iniciarSesion(@Valid @RequestBody LoginDto dto) {
        return ResponseEntity.ok(authService.iniciarSesion(dto));
    }

    @PostMapping("/recuperar-contrasena")
    public ResponseEntity<Void> recuperarContrasena(@Valid @RequestBody RecuperarContrasenaDto dto) {
        authService.solicitarRecuperacion(dto);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/cambiar-contrasena")
    public ResponseEntity<Void> cambiarContrasena(@Valid @RequestBody CambiarContrasenaDto dto) {
        authService.cambiarContrasena(dto);
        return ResponseEntity.noContent().build();
    }

    /** Actualiza la cuenta propia; la identidad sale del JWT. Devuelve un token nuevo (el correo es su sujeto). */
    @PutMapping("/me")
    public ResponseEntity<AuthRespuestaDto> actualizarPerfil(@Valid @RequestBody ActualizarPerfilDto dto,
                                                             @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(authService.actualizarPerfil(UsuarioActual.desde(jwt).id(), dto));
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioDto> perfil(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(authService.obtenerPerfil(jwt.getSubject()));
    }
}
