package com.sendera.service;

import com.sendera.dto.*;
import com.sendera.exception.CorreoYaRegistradoException;
import com.sendera.exception.CredencialesInvalidasException;
import com.sendera.exception.TokenRecuperacionInvalidoException;
import com.sendera.model.TokenRecuperacion;
import com.sendera.model.Usuario;
import com.sendera.repository.TokenRecuperacionRepository;
import com.sendera.repository.UsuarioRepository;
import com.sendera.security.JwtService;
import com.sendera.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private TokenRecuperacionRepository tokenRepository;
    @Mock private JwtService jwtService;
    @Mock private CorreoService correoService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private AuthServiceImpl service;

    @BeforeEach
    void preparar() {
        service = new AuthServiceImpl(usuarioRepository, tokenRepository, passwordEncoder, jwtService, correoService,
                new MockEnvironment());
        ReflectionTestUtils.setField(service, "frontendUrl", "http://localhost:5173");
        ReflectionTestUtils.setField(service, "minutosExpiracion", 30L);
        ReflectionTestUtils.invokeMethod(service, "inicializar");
    }

    private Usuario usuarioConClave(String clave) {
        return Usuario.builder().id(1L).nombre("María").apellidos("Gutiérrez")
                .correo("maria@correo.com").contrasenaHash(passwordEncoder.encode(clave)).build();
    }

    private RegistroDto registro() {
        RegistroDto dto = new RegistroDto();
        dto.setNombre(" María ");
        dto.setApellidos("Gutiérrez");
        dto.setCorreo("  Maria@Correo.com ");
        dto.setContrasena("clave1234");
        return dto;
    }

    @Test
    void registrarGuardaHashYNormalizaCorreo() {
        when(usuarioRepository.existsByCorreo("maria@correo.com")).thenReturn(false);
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(i -> {
            Usuario u = i.getArgument(0);
            u.setId(7L);
            return u;
        });
        when(jwtService.generar(any(Usuario.class))).thenReturn("jwt-falso");

        AuthRespuestaDto respuesta = service.registrar(registro());

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(captor.capture());
        assertEquals("maria@correo.com", captor.getValue().getCorreo());
        assertEquals("María", captor.getValue().getNombre());
        assertNotEquals("clave1234", captor.getValue().getContrasenaHash());
        assertTrue(passwordEncoder.matches("clave1234", captor.getValue().getContrasenaHash()));
        assertEquals("jwt-falso", respuesta.getToken());
        assertEquals(7L, respuesta.getUsuario().getId());
    }

    @Test
    void registrarConCorreoExistenteLanzaConflicto() {
        when(usuarioRepository.existsByCorreo("maria@correo.com")).thenReturn(true);
        assertThrows(CorreoYaRegistradoException.class, () -> service.registrar(registro()));
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void loginCorrectoDevuelveToken() {
        Usuario usuario = usuarioConClave("clave1234");
        when(usuarioRepository.findByCorreo("maria@correo.com")).thenReturn(Optional.of(usuario));
        when(jwtService.generar(usuario)).thenReturn("jwt-falso");

        LoginDto dto = new LoginDto();
        dto.setCorreo("MARIA@correo.com");
        dto.setContrasena("clave1234");

        assertEquals("jwt-falso", service.iniciarSesion(dto).getToken());
    }

    @Test
    void loginConClaveIncorrectaOCorreoInexistenteLanzaExcepcion() {
        Usuario usuario = usuarioConClave("clave1234");
        when(usuarioRepository.findByCorreo("maria@correo.com")).thenReturn(Optional.of(usuario));
        when(usuarioRepository.findByCorreo("nadie@correo.com")).thenReturn(Optional.empty());

        LoginDto incorrecta = new LoginDto();
        incorrecta.setCorreo("maria@correo.com");
        incorrecta.setContrasena("otraClave1");
        LoginDto inexistente = new LoginDto();
        inexistente.setCorreo("nadie@correo.com");
        inexistente.setContrasena("clave1234");

        assertThrows(CredencialesInvalidasException.class, () -> service.iniciarSesion(incorrecta));
        assertThrows(CredencialesInvalidasException.class, () -> service.iniciarSesion(inexistente));
    }

    @Test
    void recuperacionConCorreoInexistenteNoEnviaNada() {
        when(usuarioRepository.findByCorreo("nadie@correo.com")).thenReturn(Optional.empty());
        RecuperarContrasenaDto dto = new RecuperarContrasenaDto();
        dto.setCorreo("nadie@correo.com");

        assertDoesNotThrow(() -> service.solicitarRecuperacion(dto));
        verifyNoInteractions(correoService);
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void recuperacionGuardaSoloElHashYEnviaEnlace() {
        Usuario usuario = usuarioConClave("clave1234");
        when(usuarioRepository.findByCorreo("maria@correo.com")).thenReturn(Optional.of(usuario));
        RecuperarContrasenaDto dto = new RecuperarContrasenaDto();
        dto.setCorreo("maria@correo.com");

        service.solicitarRecuperacion(dto);

        ArgumentCaptor<TokenRecuperacion> tokenCaptor = ArgumentCaptor.forClass(TokenRecuperacion.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        ArgumentCaptor<String> enlaceCaptor = ArgumentCaptor.forClass(String.class);
        verify(correoService).enviarRecuperacionContrasena(eq("maria@correo.com"), eq("María"), enlaceCaptor.capture());

        String enlace = enlaceCaptor.getValue();
        assertTrue(enlace.startsWith("http://localhost:5173/cambiar-contrasena?token="));
        String tokenPlano = enlace.substring(enlace.indexOf("token=") + 6);
        assertEquals(64, tokenCaptor.getValue().getTokenHash().length());
        assertNotEquals(tokenPlano, tokenCaptor.getValue().getTokenHash());
    }

    @Test
    void cambiarContrasenaConTokenInexistenteFalla() {
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());
        CambiarContrasenaDto dto = new CambiarContrasenaDto();
        dto.setToken("token-inventado");
        dto.setNuevaContrasena("nueva1234");

        assertThrows(TokenRecuperacionInvalidoException.class, () -> service.cambiarContrasena(dto));
    }

    @Test
    void cambiarContrasenaConTokenExpiradoFalla() {
        TokenRecuperacion vencido = TokenRecuperacion.builder()
                .usuario(usuarioConClave("clave1234")).tokenHash("x")
                .expiraEn(LocalDateTime.now().minusMinutes(1)).build();
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(vencido));
        CambiarContrasenaDto dto = new CambiarContrasenaDto();
        dto.setToken("token");
        dto.setNuevaContrasena("nueva1234");

        assertThrows(TokenRecuperacionInvalidoException.class, () -> service.cambiarContrasena(dto));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void cambiarContrasenaValidaActualizaHashYConsumeTokens() {
        Usuario usuario = usuarioConClave("clave1234");
        TokenRecuperacion vigente = TokenRecuperacion.builder()
                .usuario(usuario).tokenHash("x").expiraEn(LocalDateTime.now().plusMinutes(10)).build();
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(vigente));
        CambiarContrasenaDto dto = new CambiarContrasenaDto();
        dto.setToken("token");
        dto.setNuevaContrasena("nueva1234");

        service.cambiarContrasena(dto);

        assertTrue(passwordEncoder.matches("nueva1234", usuario.getContrasenaHash()));
        verify(usuarioRepository).save(usuario);
        verify(tokenRepository).deleteByUsuario(usuario);
    }
}
