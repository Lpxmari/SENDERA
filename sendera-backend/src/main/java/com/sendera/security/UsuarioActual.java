package com.sendera.security;

import com.sendera.config.SecurityConfig;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

/**
 * Identidad del solicitante, tomada únicamente de los claims firmados del JWT
 * (nunca del cuerpo, la query ni el estado del frontend).
 */
public record UsuarioActual(Long id, boolean admin) {

    public static UsuarioActual desde(Jwt jwt) {
        Number uid = jwt.getClaim("uid");
        if (uid == null) {
            throw new IllegalStateException("El token no contiene el identificador de usuario.");
        }
        List<String> roles = jwt.getClaimAsStringList("roles");
        return new UsuarioActual(uid.longValue(), roles != null && roles.contains(SecurityConfig.ROL_ADMIN));
    }
}
