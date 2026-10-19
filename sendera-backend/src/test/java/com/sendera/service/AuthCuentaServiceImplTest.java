package com.sendera.service;

import com.sendera.dto.ActualizarPerfilDto;
import com.sendera.dto.AuthRespuestaDto;
import com.sendera.dto.LoginDto;
import com.sendera.dto.RegistroDto;
import com.sendera.dto.UsuarioDto;
import com.sendera.exception.ContrasenaActualIncorrectaException;
import com.sendera.exception.CorreoYaRegistradoException;
import com.sendera.exception.CredencialesInvalidasException;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** RF-001/RF-002/RF-004: registro, inicio de sesión y edición de perfil (el flujo de recuperación está en AuthServiceImplTest). */
@ExtendWith(MockitoExtension.class)
class AuthCuentaServiceImplTest {

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

    private Usuario usuario() {
        return Usuario.builder().id(7L).nombre("María").apellidos("Gutiérrez").correo("maria@correo.com")
                .rol("ROLE_CLIENTE").contrasenaHash(passwordEncoder.encode("clave1234")).build();
    }

    private RegistroDto registro(String correo) {
        RegistroDto dto = new RegistroDto();
        dto.setNombre("  María ");
        dto.setApellidos(" Gutiérrez  ");
        dto.setCorreo(correo);
        dto.setContrasena("clave1234");
        return dto;
    }

    private LoginDto login(String correo, String clave) {
        LoginDto dto = new LoginDto();
        dto.setCorreo(correo);
        dto.setContrasena(clave);
        return dto;
    }

    private ActualizarPerfilDto perfil(String correo, String contrasenaActual) {
        ActualizarPerfilDto dto = new ActualizarPerfilDto();
        dto.setNombre(" Mariana ");
        dto.setApellidos(" López ");
        dto.setCorreo(correo);
        dto.setContrasenaActual(contrasenaActual);
        return dto;
    }

    // ---------- registro ----------

    @Test
    void registrarNormalizaElCorreoGuardaSoloElHashYDevuelveElToken() {
        when(usuarioRepository.existsByCorreo("nueva@correo.com")).thenReturn(false);
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtService.generar(any(Usuario.class))).thenReturn("jwt-falso");

        AuthRespuestaDto respuesta = service.registrar(registro("  Nueva@Correo.com "));

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(guardado.capture());
        assertEquals("nueva@correo.com", guardado.getValue().getCorreo());
        assertEquals("María", guardado.getValue().getNombre());
        assertEquals("Gutiérrez", guardado.getValue().getApellidos());
        assertNotEquals("clave1234", guardado.getValue().getContrasenaHash());
        assertTrue(passwordEncoder.matches("clave1234", guardado.getValue().getContrasenaHash()));
        assertEquals("jwt-falso", respuesta.getToken());
        assertEquals("nueva@correo.com", respuesta.getUsuario().getCorreo());
    }

    @Test
    void registrarConCorreoExistenteLanzaConflictoSinGuardar() {
        when(usuarioRepository.existsByCorreo("maria@correo.com")).thenReturn(true);

        assertThrows(CorreoYaRegistradoException.class, () -> service.registrar(registro("maria@correo.com")));

        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void registrarTraduceLaCarreraDeDosRegistrosSimultaneosEnConflicto() {
        when(usuarioRepository.existsByCorreo("maria@correo.com")).thenReturn(false);
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenThrow(new DataIntegrityViolationException("duplicado"));

        assertThrows(CorreoYaRegistradoException.class, () -> service.registrar(registro("maria@correo.com")));
    }

    // ---------- inicio de sesión ----------

    @Test
    void iniciarSesionCorrectoDevuelveElToken() {
        Usuario usuario = usuario();
        when(usuarioRepository.findByCorreo("maria@correo.com")).thenReturn(Optional.of(usuario));
        when(jwtService.generar(usuario)).thenReturn("jwt-falso");

        AuthRespuestaDto respuesta = service.iniciarSesion(login(" MARIA@correo.com ", "clave1234"));

        assertEquals("jwt-falso", respuesta.getToken());
        assertEquals(7L, respuesta.getUsuario().getId());
    }

    @Test
    void iniciarSesionFallaIgualConContrasenaErradaCorreoInexistenteOCuentaInactiva() {
        Usuario inactivo = usuario();
        inactivo.setActivo(false);
        when(usuarioRepository.findByCorreo("maria@correo.com")).thenReturn(Optional.of(usuario()));
        when(usuarioRepository.findByCorreo("nadie@correo.com")).thenReturn(Optional.empty());
        when(usuarioRepository.findByCorreo("inactivo@correo.com")).thenReturn(Optional.of(inactivo));

        assertThrows(CredencialesInvalidasException.class, () -> service.iniciarSesion(login("maria@correo.com", "otra-clave1")));
        assertThrows(CredencialesInvalidasException.class, () -> service.iniciarSesion(login("nadie@correo.com", "clave1234")));
        assertThrows(CredencialesInvalidasException.class, () -> service.iniciarSesion(login("inactivo@correo.com", "clave1234")));
        verifyNoInteractions(jwtService);
    }

    // ---------- perfil ----------

    @Test
    void obtenerPerfilDevuelveLosDatosSinElHash() {
        when(usuarioRepository.findByCorreo("maria@correo.com")).thenReturn(Optional.of(usuario()));

        UsuarioDto dto = service.obtenerPerfil("maria@correo.com");

        assertEquals("María", dto.getNombre());
        assertEquals("ROLE_CLIENTE", dto.getRol());
    }

    @Test
    void obtenerPerfilDeCuentaInexistenteFalla() {
        when(usuarioRepository.findByCorreo("nadie@correo.com")).thenReturn(Optional.empty());

        assertThrows(CredencialesInvalidasException.class, () -> service.obtenerPerfil("nadie@correo.com"));
    }

    @Test
    void actualizarPerfilSinCambiarCorreoNoPideContrasena() {
        Usuario usuario = usuario();
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.saveAndFlush(usuario)).thenReturn(usuario);
        when(jwtService.generar(usuario)).thenReturn("jwt-nuevo");

        AuthRespuestaDto respuesta = service.actualizarPerfil(7L, perfil("MARIA@correo.com", null));

        assertEquals("Mariana", usuario.getNombre());
        assertEquals("López", usuario.getApellidos());
        assertEquals("maria@correo.com", usuario.getCorreo());
        assertEquals("jwt-nuevo", respuesta.getToken());
    }

    @Test
    void cambiarElCorreoExigeLaContrasenaActual() {
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario()));

        assertThrows(ContrasenaActualIncorrectaException.class, () -> service.actualizarPerfil(7L, perfil("otro@correo.com", null)));
        assertThrows(ContrasenaActualIncorrectaException.class, () -> service.actualizarPerfil(7L, perfil("otro@correo.com", "  ")));
        assertThrows(ContrasenaActualIncorrectaException.class, () -> service.actualizarPerfil(7L, perfil("otro@correo.com", "incorrecta1")));
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void cambiarElCorreoAUnoYaRegistradoLanzaConflicto() {
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario()));
        when(usuarioRepository.existsByCorreo("otro@correo.com")).thenReturn(true);

        assertThrows(CorreoYaRegistradoException.class, () -> service.actualizarPerfil(7L, perfil("otro@correo.com", "clave1234")));
    }

    @Test
    void cambiarElCorreoConLaContrasenaCorrectaLoActualiza() {
        Usuario usuario = usuario();
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByCorreo("otro@correo.com")).thenReturn(false);
        when(usuarioRepository.saveAndFlush(usuario)).thenReturn(usuario);
        when(jwtService.generar(usuario)).thenReturn("jwt-nuevo");

        service.actualizarPerfil(7L, perfil("otro@correo.com", "clave1234"));

        assertEquals("otro@correo.com", usuario.getCorreo());
    }

    @Test
    void siOtraCuentaTomaElCorreoEntreLaComprobacionYElGuardadoHayConflicto() {
        Usuario usuario = usuario();
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByCorreo("otro@correo.com")).thenReturn(false);
        when(usuarioRepository.saveAndFlush(usuario)).thenThrow(new DataIntegrityViolationException("duplicado"));

        assertThrows(CorreoYaRegistradoException.class, () -> service.actualizarPerfil(7L, perfil("otro@correo.com", "clave1234")));
    }

    @Test
    void actualizarPerfilDeCuentaInexistenteFalla() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(CredencialesInvalidasException.class, () -> service.actualizarPerfil(99L, perfil("maria@correo.com", null)));
    }
}
