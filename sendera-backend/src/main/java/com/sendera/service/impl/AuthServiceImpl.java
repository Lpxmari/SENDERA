package com.sendera.service.impl;

import com.sendera.dto.*;
import com.sendera.exception.CorreoYaRegistradoException;
import com.sendera.exception.CredencialesInvalidasException;
import com.sendera.exception.TokenRecuperacionInvalidoException;
import com.sendera.model.TokenRecuperacion;
import com.sendera.model.Usuario;
import com.sendera.repository.TokenRecuperacionRepository;
import com.sendera.repository.UsuarioRepository;
import com.sendera.security.JwtService;
import com.sendera.service.AuthService;
import com.sendera.service.CorreoService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final TokenRecuperacionRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CorreoService correoService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.recuperacion.expiracion-minutos}")
    private long minutosExpiracion;

    /** Hash de relleno para que el login tarde igual si el correo no existe. */
    private String hashFicticio;

    @PostConstruct
    void inicializar() {
        hashFicticio = passwordEncoder.encode("sendera-relleno-" + Long.toHexString(RANDOM.nextLong()));
    }

    @Override
    @Transactional
    public AuthRespuestaDto registrar(RegistroDto dto) {
        String correo = normalizar(dto.getCorreo());
        if (usuarioRepository.existsByCorreo(correo)) {
            throw new CorreoYaRegistradoException();
        }

        Usuario usuario = Usuario.builder()
                .nombre(dto.getNombre().trim())
                .apellidos(dto.getApellidos().trim())
                .correo(correo)
                .contrasenaHash(passwordEncoder.encode(dto.getContrasena()))
                .build();
        try {
            usuario = usuarioRepository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException e) {
            // Dos registros simultáneos con el mismo correo
            throw new CorreoYaRegistradoException();
        }
        return construirRespuesta(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthRespuestaDto iniciarSesion(LoginDto dto) {
        Usuario usuario = usuarioRepository.findByCorreo(normalizar(dto.getCorreo())).orElse(null);
        String hash = usuario != null ? usuario.getContrasenaHash() : hashFicticio;
        boolean coincide = passwordEncoder.matches(dto.getContrasena(), hash);
        if (usuario == null || !coincide) {
            throw new CredencialesInvalidasException();
        }
        return construirRespuesta(usuario);
    }

    @Override
    @Transactional
    public void solicitarRecuperacion(RecuperarContrasenaDto dto) {
        usuarioRepository.findByCorreo(normalizar(dto.getCorreo())).ifPresent(usuario -> {
            // Solo un enlace vigente por usuario
            tokenRepository.deleteByUsuario(usuario);

            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

            tokenRepository.save(TokenRecuperacion.builder()
                    .usuario(usuario)
                    .tokenHash(hash(token))
                    .expiraEn(LocalDateTime.now().plusMinutes(minutosExpiracion))
                    .build());

            String enlace = UriComponentsBuilder.fromUriString(frontendUrl)
                    .path("/cambiar-contrasena")
                    .queryParam("token", token)
                    .build().toUriString();
            correoService.enviarRecuperacionContrasena(usuario.getCorreo(), usuario.getNombre(), enlace);
        });
    }

    @Override
    @Transactional
    public void cambiarContrasena(CambiarContrasenaDto dto) {
        TokenRecuperacion token = tokenRepository.findByTokenHash(hash(dto.getToken()))
                .orElseThrow(TokenRecuperacionInvalidoException::new);

        if (token.getExpiraEn().isBefore(LocalDateTime.now())) {
            tokenRepository.delete(token);
            throw new TokenRecuperacionInvalidoException();
        }

        Usuario usuario = token.getUsuario();
        usuario.setContrasenaHash(passwordEncoder.encode(dto.getNuevaContrasena()));
        usuarioRepository.save(usuario);
        // El enlace es de un solo uso: se eliminan todos los tokens del usuario
        tokenRepository.deleteByUsuario(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioDto obtenerPerfil(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(CredencialesInvalidasException::new);
        return convertirADto(usuario);
    }

    private AuthRespuestaDto construirRespuesta(Usuario usuario) {
        return AuthRespuestaDto.builder()
                .token(jwtService.generar(usuario))
                .usuario(convertirADto(usuario))
                .build();
    }

    private UsuarioDto convertirADto(Usuario usuario) {
        return UsuarioDto.builder()
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .apellidos(usuario.getApellidos())
                .correo(usuario.getCorreo())
                .build();
    }

    private String normalizar(String correo) {
        return correo.trim().toLowerCase(Locale.ROOT);
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
