package com.sendera.service;

import com.sendera.dto.CambiarActivoDto;
import com.sendera.dto.CambiarRolDto;
import com.sendera.dto.UsuarioAdminDto;
import com.sendera.exception.FiltroInvalidoException;
import com.sendera.exception.OperacionAdminNoPermitidaException;
import com.sendera.exception.RecursoNoEncontradoException;
import com.sendera.model.Usuario;
import com.sendera.repository.UsuarioRepository;
import com.sendera.security.UsuarioActual;
import com.sendera.service.impl.UsuarioAdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** RF-013: reglas de la gestión administrativa de usuarios y su auditoría. */
@ExtendWith(MockitoExtension.class)
class UsuarioAdminServiceImplTest {

    private static final UsuarioActual ADMIN = new UsuarioActual(1L, true);

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AuditoriaService auditoriaService;

    private UsuarioAdminServiceImpl service;

    @BeforeEach
    void preparar() {
        service = new UsuarioAdminServiceImpl(usuarioRepository, auditoriaService);
    }

    private Usuario usuario(long id, String rol, boolean activo) {
        return Usuario.builder().id(id).nombre("Ana").apellidos("Pérez").correo("u" + id + "@correo.com")
                .contrasenaHash("hash").rol(rol).activo(activo).build();
    }

    private CambiarRolDto rol(String valor) {
        CambiarRolDto dto = new CambiarRolDto();
        dto.setRol(valor);
        return dto;
    }

    private CambiarActivoDto activo(boolean valor) {
        CambiarActivoDto dto = new CambiarActivoDto();
        dto.setActivo(valor);
        return dto;
    }

    @Test
    void cambiarRolPersisteYAudita() {
        Usuario cliente = usuario(2L, "ROLE_CLIENTE", true);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(cliente));

        UsuarioAdminDto resultado = service.cambiarRol(2L, rol("ROLE_ADMIN"), ADMIN);

        assertEquals("ROLE_ADMIN", resultado.getRol());
        verify(usuarioRepository).save(cliente);
        verify(auditoriaService).registrar(ADMIN, "USUARIO_CAMBIAR_ROL", "usuario", 2L, "ROLE_CLIENTE -> ROLE_ADMIN");
    }

    @Test
    void noPermiteCambiarElPropioRol() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario(1L, "ROLE_ADMIN", true)));

        assertThrows(OperacionAdminNoPermitidaException.class, () -> service.cambiarRol(1L, rol("ROLE_CLIENTE"), ADMIN));
        verify(usuarioRepository, never()).save(any());
        verify(auditoriaService, never()).registrar(any(), anyString(), anyString(), anyLong(), any());
    }

    @Test
    void noPermiteQuitarElRolAlUltimoAdministradorActivo() {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario(2L, "ROLE_ADMIN", true)));
        when(usuarioRepository.countByRolAndActivoTrue("ROLE_ADMIN")).thenReturn(1L);

        assertThrows(OperacionAdminNoPermitidaException.class, () -> service.cambiarRol(2L, rol("ROLE_CLIENTE"), ADMIN));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void cambiarAlMismoRolNoGuardaNiAudita() {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario(2L, "ROLE_CLIENTE", true)));

        service.cambiarRol(2L, rol("ROLE_CLIENTE"), ADMIN);

        verify(usuarioRepository, never()).save(any());
        verify(auditoriaService, never()).registrar(any(), anyString(), anyString(), anyLong(), any());
    }

    @Test
    void desactivarUsuarioAudita() {
        Usuario cliente = usuario(2L, "ROLE_CLIENTE", true);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(cliente));

        UsuarioAdminDto resultado = service.cambiarActivo(2L, activo(false), ADMIN);

        assertFalse(resultado.isActivo());
        verify(auditoriaService).registrar(ADMIN, "USUARIO_DESACTIVAR", "usuario", 2L, null);
    }

    @Test
    void reactivarUsuarioAudita() {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario(2L, "ROLE_CLIENTE", false)));

        UsuarioAdminDto resultado = service.cambiarActivo(2L, activo(true), ADMIN);

        assertTrue(resultado.isActivo());
        verify(auditoriaService).registrar(ADMIN, "USUARIO_ACTIVAR", "usuario", 2L, null);
    }

    @Test
    void noPermiteDesactivarLaPropiaCuenta() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario(1L, "ROLE_ADMIN", true)));

        assertThrows(OperacionAdminNoPermitidaException.class, () -> service.cambiarActivo(1L, activo(false), ADMIN));
    }

    @Test
    void noPermiteDesactivarAlUltimoAdministradorActivo() {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario(2L, "ROLE_ADMIN", true)));
        when(usuarioRepository.countByRolAndActivoTrue("ROLE_ADMIN")).thenReturn(1L);

        assertThrows(OperacionAdminNoPermitidaException.class, () -> service.cambiarActivo(2L, activo(false), ADMIN));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void usuarioInexistenteDevuelveNoEncontrado() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.obtener(99L));
        assertThrows(RecursoNoEncontradoException.class, () -> service.cambiarRol(99L, rol("ROLE_ADMIN"), ADMIN));
    }

    @Test
    void listarValidaPaginacionYRol() {
        assertThrows(FiltroInvalidoException.class, () -> service.listar(null, null, null, -1, 20));
        assertThrows(FiltroInvalidoException.class, () -> service.listar(null, null, null, 0, 0));
        assertThrows(FiltroInvalidoException.class, () -> service.listar(null, null, null, 0, 101));
        assertThrows(FiltroInvalidoException.class, () -> service.listar(null, "ROLE_SUPERUSUARIO", null, 0, 20));
    }

    @Test
    void listarEscapaLosComodinesDeLaBusqueda() {
        Page<Usuario> pagina = new PageImpl<>(List.of(usuario(2L, "ROLE_CLIENTE", true)));
        when(usuarioRepository.buscar(anyString(), isNull(), isNull(), any(Pageable.class))).thenReturn(pagina);

        service.listar(" 50%_Ana ", null, null, 0, 20);

        ArgumentCaptor<String> patron = ArgumentCaptor.forClass(String.class);
        verify(usuarioRepository).buscar(patron.capture(), isNull(), isNull(), any(Pageable.class));
        assertEquals("%50!%!_ana%", patron.getValue());
    }

    @Test
    void listarSinTextoNoFiltraPorPatron() {
        when(usuarioRepository.buscar(isNull(), eq("ROLE_ADMIN"), eq(true), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        assertEquals(0, service.listar("  ", "role_admin", true, 0, 20).getTotal());
    }
}
