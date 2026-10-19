package com.sendera.service.impl;

import com.sendera.dto.*;
import com.sendera.exception.ContrasenaActualIncorrectaException;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MAX_INTENTOS = 5;
    private static final long ESPERA_REENVIO_SEGUNDOS = 60;

    private final UsuarioRepository usuarioRepository;
    private final TokenRecuperacionRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CorreoService correoService;
    private final Environment entorno;

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
        // Una cuenta desactivada recibe el mismo error genérico: no se revela su estado.
        if (usuario == null || !coincide || !usuario.isActivo()) {
            throw new CredencialesInvalidasException();
        }
        return construirRespuesta(usuario);
    }

    @Override
    @Transactional
    public void solicitarRecuperacion(RecuperarContrasenaDto dto) {
        String correoSolicitado = normalizar(dto.getCorreo());
        Usuario usuario = usuarioRepository.findByCorreo(correoSolicitado).orElse(null);

        if (usuario == null || !usuario.isActivo()) {
            passwordEncoder.matches("000000", hashFicticio);
            if (entorno.acceptsProfiles(Profiles.of("dev"))) {
                log.warn("Recuperación solicitada para un correo NO registrado o inactivo: {}. No se genera código.", correoSolicitado);
            }
            return;
        }

        LocalDateTime ahora = LocalDateTime.now();
        TokenRecuperacion ultimo = tokenRepository.findFirstByUsuarioOrderByIdDesc(usuario).orElse(null);
        if (ultimo != null && ultimo.getCreadoEn().isAfter(ahora.minusSeconds(ESPERA_REENVIO_SEGUNDOS))) {
            return;
        }

        tokenRepository.deleteByUsuario(usuario);
        String codigo = String.format("%06d", RANDOM.nextInt(1_000_000));
        tokenRepository.save(TokenRecuperacion.builder()
                .usuario(usuario)
                .codigoHash(passwordEncoder.encode(codigo))
                .expiraEn(ahora.plusMinutes(minutosExpiracion))
                .intentos(0)
                .build());

        correoService.enviarCodigoRecuperacion(usuario.getCorreo(), usuario.getNombre(), codigo);
    }

    @Override
    @Transactional(noRollbackFor = TokenRecuperacionInvalidoException.class)
    public void verificarCodigo(VerificarCodigoDto dto) {
        validarCodigo(dto.getCorreo(), dto.getCodigo());
    }

    @Override
    @Transactional(noRollbackFor = TokenRecuperacionInvalidoException.class)
    public void cambiarContrasena(CambiarContrasenaDto dto) {
        TokenRecuperacion token = validarCodigo(dto.getCorreo(), dto.getCodigo());
        Usuario usuario = token.getUsuario();
        usuario.setContrasenaHash(passwordEncoder.encode(dto.getNuevaContrasena()));
        usuarioRepository.save(usuario);
        tokenRepository.deleteByUsuario(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioDto obtenerPerfil(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(CredencialesInvalidasException::new);
        return convertirADto(usuario);
    }

    @Override
    @Transactional
    public AuthRespuestaDto actualizarPerfil(Long usuarioId, ActualizarPerfilDto dto) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow(CredencialesInvalidasException::new);
        String correoNuevo = normalizar(dto.getCorreo());

        if (!correoNuevo.equals(usuario.getCorreo())) {
            String actual = dto.getContrasenaActual();
            if (actual == null || actual.isBlank() || !passwordEncoder.matches(actual, usuario.getContrasenaHash())) {
                throw new ContrasenaActualIncorrectaException();
            }
            if (usuarioRepository.existsByCorreo(correoNuevo)) {
                throw new CorreoYaRegistradoException();
            }
            usuario.setCorreo(correoNuevo);
        }
        usuario.setNombre(dto.getNombre().trim());
        usuario.setApellidos(dto.getApellidos().trim());
        try {
            usuario = usuarioRepository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException e) {
            // Otra cuenta tomó ese correo entre la comprobación y el guardado
            throw new CorreoYaRegistradoException();
        }
        return construirRespuesta(usuario);
    }

    private TokenRecuperacion validarCodigo(String correo, String codigo) {
        Usuario usuario = usuarioRepository.findByCorreo(normalizar(correo)).orElse(null);
        TokenRecuperacion token = usuario == null ? null : tokenRepository.findFirstByUsuarioOrderByIdDesc(usuario).orElse(null);

        if (token == null) {
            passwordEncoder.matches(codigo, hashFicticio);
            throw new TokenRecuperacionInvalidoException();
        }

        if (!token.getExpiraEn().isAfter(LocalDateTime.now())) {
            tokenRepository.delete(token);
            throw new TokenRecuperacionInvalidoException();
        }

        int intentosAntes = token.getIntentos();
        if (tokenRepository.consumirIntento(token.getId(), MAX_INTENTOS) == 0) {
            tokenRepository.delete(token);
            throw new TokenRecuperacionInvalidoException("Ingresaste un código incorrecto demasiadas veces. Solicita un código nuevo.");
        }

        if (!passwordEncoder.matches(codigo, token.getCodigoHash())) {
            if (intentosAntes + 1 >= MAX_INTENTOS) {
                tokenRepository.delete(token);
                throw new TokenRecuperacionInvalidoException("Ingresaste un código incorrecto demasiadas veces. Solicita un código nuevo.");
            }
            throw new TokenRecuperacionInvalidoException();
        }

        tokenRepository.devolverIntento(token.getId());
        return token;
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
                .rol(usuario.getRol())
                .build();
    }

    private String normalizar(String correo) {
        return correo.trim().toLowerCase(Locale.ROOT);
    }
}
