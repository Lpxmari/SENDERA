package com.sendera.security;

import com.sendera.model.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/** Genera los tokens JWT (HS256) que el frontend envía en el encabezado Authorization. */
@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final long expiracionHoras;

    public JwtService(JwtEncoder jwtEncoder, @Value("${app.jwt.expiracion-horas}") long expiracionHoras) {
        this.jwtEncoder = jwtEncoder;
        this.expiracionHoras = expiracionHoras;
    }

    public String generar(Usuario usuario) {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("sendera")
                .subject(usuario.getCorreo())
                .issuedAt(ahora)
                .expiresAt(ahora.plus(Duration.ofHours(expiracionHoras)))
                .claim("uid", usuario.getId())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
