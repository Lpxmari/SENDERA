package com.sendera.service;

import com.sendera.dto.*;
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
        ReflectionTestUtils.setField(service, "minutosExpiracion", 15L);
        ReflectionTestUtils.invokeMethod(service, "inicializar");
    }

    private Usuario usuarioConClave(String clave) {
        return Usuario.builder().id(1L).nombre("María").apellidos("Gutiérrez")
                .correo("maria@correo.com").contrasenaHash(passwordEncoder.encode(clave)).build();
    }

    private RecuperarContrasenaDto recuperar(String correo) {
        RecuperarContrasenaDto dto = new RecuperarContrasenaDto();
        dto.setCorreo(correo);
        return dto;
    }

    @Test
    void recuperacionConCorreoInexistenteOInactivoNoGuardaNiEnviaNada() {
        when(usuarioRepository.findByCorreo("nadie@correo.com")).thenReturn(Optional.empty());
        when(usuarioRepository.findByCorreo("inactivo@correo.com")).thenReturn(Optional.of(usuarioConClave("clave1234")));
        Usuario inactivo = usuarioConClave("clave1234");
        inactivo.setActivo(false);
        when(usuarioRepository.findByCorreo("inactivo@correo.com")).thenReturn(Optional.of(inactivo));

        assertDoesNotThrow(() -> service.solicitarRecuperacion(recuperar("nadie@correo.com")));
        assertDoesNotThrow(() -> service.solicitarRecuperacion(recuperar("inactivo@correo.com")));

        verify(tokenRepository, never()).save(any());
        verify(correoService, never()).enviarCodigoRecuperacion(anyString(), anyString(), anyString());
    }

    @Test
    void solicitudValidaGuardaHashDelCodigoYEnviaCodigoDeSeisDigitos() {
        Usuario usuario = usuarioConClave("clave1234");
        when(usuarioRepository.findByCorreo("maria@correo.com")).thenReturn(Optional.of(usuario));
        when(tokenRepository.findFirstByUsuarioOrderByIdDesc(usuario)).thenReturn(Optional.empty());
        when(tokenRepository.save(any(TokenRecuperacion.class))).thenAnswer(i -> i.getArgument(0));

        service.solicitarRecuperacion(recuperar("maria@correo.com"));

        ArgumentCaptor<TokenRecuperacion> captor = ArgumentCaptor.forClass(TokenRecuperacion.class);
        verify(tokenRepository).save(captor.capture());
        TokenRecuperacion guardado = captor.getValue();
        assertNotNull(guardado.getCodigoHash());
        assertEquals(60, guardado.getCodigoHash().length());
        ArgumentCaptor<String> codigoCaptor = ArgumentCaptor.forClass(String.class);
        verify(correoService).enviarCodigoRecuperacion(eq("maria@correo.com"), eq("María"), codigoCaptor.capture());
        String codigo = codigoCaptor.getValue();
        assertTrue(codigo.matches("\\d{6}"));
        assertTrue(passwordEncoder.matches(codigo, guardado.getCodigoHash()));
    }

    @Test
    void segundaSolicitudAntesDe60SegundosNoEnviaOtroCodigo() {
        Usuario usuario = usuarioConClave("clave1234");
        when(usuarioRepository.findByCorreo("maria@correo.com")).thenReturn(Optional.of(usuario));
        when(tokenRepository.findFirstByUsuarioOrderByIdDesc(usuario)).thenReturn(Optional.of(
                TokenRecuperacion.builder().usuario(usuario).codigoHash(passwordEncoder.encode("123456"))
                        .expiraEn(LocalDateTime.now().plusMinutes(10)).creadoEn(LocalDateTime.now()).build()));

        assertDoesNotThrow(() -> service.solicitarRecuperacion(recuperar("maria@correo.com")));
        verify(correoService, never()).enviarCodigoRecuperacion(anyString(), anyString(), anyString());
    }

    @Test
    void verificarCodigoCorrectoLlamaDevolverIntentoYNoBorraElCodigo() {
        Usuario usuario = usuarioConClave("clave1234");
        TokenRecuperacion token = TokenRecuperacion.builder().id(4L).usuario(usuario)
                .codigoHash(passwordEncoder.encode("123456")).expiraEn(LocalDateTime.now().plusMinutes(5)).build();
        when(usuarioRepository.findByCorreo("maria@correo.com")).thenReturn(Optional.of(usuario));
        when(tokenRepository.findFirstByUsuarioOrderByIdDesc(usuario)).thenReturn(Optional.of(token));
        when(tokenRepository.consumirIntento(4L, 5)).thenReturn(1);
        when(tokenRepository.devolverIntento(4L)).thenReturn(1);

        assertDoesNotThrow(() -> service.verificarCodigo(new VerificarCodigoDto() {{ setCorreo("maria@correo.com"); setCodigo("123456"); }}));
        verify(tokenRepository).devolverIntento(4L);
        verify(tokenRepository, never()).delete(any());
    }

    @Test
    void codigoIncorrectoLanzaExcepcion() {
        Usuario usuario = usuarioConClave("clave1234");
        TokenRecuperacion token = TokenRecuperacion.builder().id(4L).usuario(usuario)
                .codigoHash(passwordEncoder.encode("123456")).expiraEn(LocalDateTime.now().plusMinutes(5)).build();
        when(usuarioRepository.findByCorreo("maria@correo.com")).thenReturn(Optional.of(usuario));
        when(tokenRepository.findFirstByUsuarioOrderByIdDesc(usuario)).thenReturn(Optional.of(token));
        when(tokenRepository.consumirIntento(4L, 5)).thenReturn(1);

        VerificarCodigoDto dto = new VerificarCodigoDto();
        dto.setCorreo("maria@correo.com");
        dto.setCodigo("111111");

        assertThrows(TokenRecuperacionInvalidoException.class, () -> service.verificarCodigo(dto));
    }

    @Test
    void elQuintoIntentoFallidoBorraElCodigoYUsaElMensajeCorrecto() {
        Usuario usuario = usuarioConClave("clave1234");
        TokenRecuperacion token = TokenRecuperacion.builder().id(4L).usuario(usuario)
                .intentos(4)
                .codigoHash(passwordEncoder.encode("123456")).expiraEn(LocalDateTime.now().plusMinutes(5)).build();
        when(usuarioRepository.findByCorreo("maria@correo.com")).thenReturn(Optional.of(usuario));
        when(tokenRepository.findFirstByUsuarioOrderByIdDesc(usuario)).thenReturn(Optional.of(token));
        when(tokenRepository.consumirIntento(4L, 5)).thenReturn(1);

        VerificarCodigoDto dto = new VerificarCodigoDto();
        dto.setCorreo("maria@correo.com");
        dto.setCodigo("999999");

        TokenRecuperacionInvalidoException ex = assertThrows(TokenRecuperacionInvalidoException.class,
                () -> service.verificarCodigo(dto));
        assertEquals("Ingresaste un código incorrecto demasiadas veces. Solicita un código nuevo.", ex.getMessage());
        verify(tokenRepository).delete(token);
    }

    @Test
    void siConsumirIntentoDevuelveCeroSeRechazaAunqueElCodigoSeaCorrecto() {
        Usuario usuario = usuarioConClave("clave1234");
        TokenRecuperacion token = TokenRecuperacion.builder().id(4L).usuario(usuario)
                .codigoHash(passwordEncoder.encode("123456")).expiraEn(LocalDateTime.now().plusMinutes(5)).build();
        when(usuarioRepository.findByCorreo("maria@correo.com")).thenReturn(Optional.of(usuario));
        when(tokenRepository.findFirstByUsuarioOrderByIdDesc(usuario)).thenReturn(Optional.of(token));
        when(tokenRepository.consumirIntento(4L, 5)).thenReturn(0);

        VerificarCodigoDto dto = new VerificarCodigoDto();
        dto.setCorreo("maria@correo.com");
        dto.setCodigo("123456");

        assertThrows(TokenRecuperacionInvalidoException.class, () -> service.verificarCodigo(dto));
        verify(tokenRepository).delete(token);
    }

    @Test
    void codigoVencidoFallaYSeBorra() {
        Usuario usuario = usuarioConClave("clave1234");
        TokenRecuperacion token = TokenRecuperacion.builder().id(4L).usuario(usuario)
                .codigoHash(passwordEncoder.encode("123456")).expiraEn(LocalDateTime.now().minusMinutes(1)).build();
        when(usuarioRepository.findByCorreo("maria@correo.com")).thenReturn(Optional.of(usuario));
        when(tokenRepository.findFirstByUsuarioOrderByIdDesc(usuario)).thenReturn(Optional.of(token));

        VerificarCodigoDto dto = new VerificarCodigoDto();
        dto.setCorreo("maria@correo.com");
        dto.setCodigo("123456");

        assertThrows(TokenRecuperacionInvalidoException.class, () -> service.verificarCodigo(dto));
        verify(tokenRepository).delete(token);
    }

    @Test
    void cambiarContrasenaConCodigoValidoActualizaHashYEliminaTokens() {
        Usuario usuario = usuarioConClave("clave1234");
        TokenRecuperacion token = TokenRecuperacion.builder().id(4L).usuario(usuario)
                .codigoHash(passwordEncoder.encode("123456")).expiraEn(LocalDateTime.now().plusMinutes(5)).build();
        when(usuarioRepository.findByCorreo("maria@correo.com")).thenReturn(Optional.of(usuario));
        when(tokenRepository.findFirstByUsuarioOrderByIdDesc(usuario)).thenReturn(Optional.of(token));
        when(tokenRepository.consumirIntento(4L, 5)).thenReturn(1);
        when(tokenRepository.devolverIntento(4L)).thenReturn(1);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        CambiarContrasenaDto dto = new CambiarContrasenaDto();
        dto.setCorreo("maria@correo.com");
        dto.setCodigo("123456");
        dto.setNuevaContrasena("nueva1234");

        assertDoesNotThrow(() -> service.cambiarContrasena(dto));
        assertTrue(passwordEncoder.matches("nueva1234", usuario.getContrasenaHash()));
        verify(usuarioRepository).save(usuario);
        verify(tokenRepository).deleteByUsuario(usuario);
    }
}
