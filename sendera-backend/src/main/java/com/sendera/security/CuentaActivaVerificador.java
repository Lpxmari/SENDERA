package com.sendera.security;

import com.sendera.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Comprueba en cada petición autenticada que la cuenta siga activa, para que retirar el acceso
 * a un usuario surta efecto de inmediato y no solo cuando venza su JWT.
 */
@Component
@RequiredArgsConstructor
public class CuentaActivaVerificador {

    private final UsuarioRepository usuarioRepository;

    public boolean estaActiva(Long usuarioId) {
        return usuarioRepository.existsByIdAndActivoTrue(usuarioId);
    }
}
