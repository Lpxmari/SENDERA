package com.sendera.service;

import com.sendera.dto.CambiarActivoDto;
import com.sendera.dto.CambiarRolDto;
import com.sendera.dto.PaginaDto;
import com.sendera.dto.UsuarioAdminDto;
import com.sendera.exception.OperacionAdminNoPermitidaException;
import com.sendera.model.AuditoriaAdmin;
import com.sendera.model.Usuario;
import com.sendera.repository.AuditoriaAdminRepository;
import com.sendera.repository.UsuarioRepository;
import com.sendera.security.UsuarioActual;
import com.sendera.service.impl.AuditoriaServiceImpl;
import com.sendera.service.impl.UsuarioAdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** RF-013 contra H2: búsqueda real, estado de la cuenta y registro de auditoría persistido. */
@DataJpaTest
@ActiveProfiles("test")
@Import({UsuarioAdminServiceImpl.class, AuditoriaServiceImpl.class})
class UsuarioAdminIntegracionTest {

    @Autowired TestEntityManager em;
    @Autowired UsuarioAdminService service;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired AuditoriaAdminRepository auditoriaRepository;

    private UsuarioActual admin;
    private Usuario cliente;

    @BeforeEach
    void setUp() {
        admin = new UsuarioActual(crear("admin@sendera.com", "Ada", "Admin", "ROLE_ADMIN").getId(), true);
        cliente = crear("cliente@sendera.com", "Carlos", "100%_Real", "ROLE_CLIENTE");
        crear("otra@sendera.com", "Olga", "Gómez", "ROLE_CLIENTE");
        em.flush();
    }

    private Usuario crear(String correo, String nombre, String apellidos, String rol) {
        return em.persist(Usuario.builder().nombre(nombre).apellidos(apellidos).correo(correo)
                .contrasenaHash("hash").rol(rol).build());
    }

    @Test
    void lasCuentasNuevasNacenActivas() {
        assertTrue(usuarioRepository.existsByIdAndActivoTrue(cliente.getId()));
    }

    @Test
    void buscaPorNombreApellidoOCorreoSinDistinguirMayusculas() {
        assertEquals(1, service.listar("CARLOS", null, null, 0, 20).getTotal());
        assertEquals(1, service.listar("gómez", null, null, 0, 20).getTotal());
        assertEquals(3, service.listar("sendera.com", null, null, 0, 20).getTotal());
    }

    @Test
    void losComodinesDeLaBusquedaSeTratanComoTextoLiteral() {
        PaginaDto<UsuarioAdminDto> literal = service.listar("100%_", null, null, 0, 20);
        assertEquals(1, literal.getTotal());
        assertEquals(0, service.listar("%", null, null, 0, 20).getContenido().stream()
                .filter(u -> u.getCorreo().startsWith("admin")).count());
    }

    @Test
    void filtraPorRolYEstadoYPagina() {
        assertEquals(2, service.listar(null, "ROLE_CLIENTE", true, 0, 20).getTotal());
        assertEquals(0, service.listar(null, "ROLE_CLIENTE", false, 0, 20).getTotal());
        PaginaDto<UsuarioAdminDto> primera = service.listar(null, null, null, 0, 2);
        assertEquals(2, primera.getContenido().size());
        assertEquals(3, primera.getTotal());
    }

    @Test
    void desactivarRetiraElAccesoYQuedaAuditado() {
        service.cambiarActivo(cliente.getId(), activo(false), admin);
        em.flush();
        em.clear();

        assertFalse(usuarioRepository.existsByIdAndActivoTrue(cliente.getId()));
        List<AuditoriaAdmin> registros = auditoriaRepository.findAll();
        assertEquals(1, registros.size());
        assertEquals("USUARIO_DESACTIVAR", registros.get(0).getAccion());
        assertEquals(admin.id(), registros.get(0).getAdminId());
        assertEquals(cliente.getId(), registros.get(0).getRecursoId());
        assertNotNull(registros.get(0).getCreadaEn());
    }

    @Test
    void cambiarRolPersiste() {
        CambiarRolDto dto = new CambiarRolDto();
        dto.setRol("ROLE_ADMIN");

        service.cambiarRol(cliente.getId(), dto, admin);
        em.flush();
        em.clear();

        assertEquals("ROLE_ADMIN", usuarioRepository.findById(cliente.getId()).orElseThrow().getRol());
        assertEquals(2, usuarioRepository.countByRolAndActivoTrue("ROLE_ADMIN"));
    }

    @Test
    void elUnicoAdministradorNoPuedeSerDesactivadoPorOtroAdministradorInactivo() {
        // El único administrador activo es 'admin'; otro actor (id distinto) intenta desactivarlo.
        UsuarioActual otro = new UsuarioActual(cliente.getId(), true);
        assertThrows(OperacionAdminNoPermitidaException.class,
                () -> service.cambiarActivo(admin.id(), activo(false), otro));
    }

    private CambiarActivoDto activo(boolean valor) {
        CambiarActivoDto dto = new CambiarActivoDto();
        dto.setActivo(valor);
        return dto;
    }
}
