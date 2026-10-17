package com.sendera.service;

import com.sendera.dto.PaginaDto;
import com.sendera.dto.ResenaAdminDto;
import com.sendera.exception.FiltroInvalidoException;
import com.sendera.exception.RecursoNoEncontradoException;
import com.sendera.model.AuditoriaAdmin;
import com.sendera.model.Destino;
import com.sendera.model.Resena;
import com.sendera.model.Usuario;
import com.sendera.repository.AuditoriaAdminRepository;
import com.sendera.repository.ResenaRepository;
import com.sendera.security.UsuarioActual;
import com.sendera.service.impl.AuditoriaServiceImpl;
import com.sendera.service.impl.ResenaAdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** RF-013 contra H2: listado de moderación sin cargar imágenes, filtros y eliminación auditada. */
@DataJpaTest
@ActiveProfiles("test")
@Import({ResenaAdminServiceImpl.class, AuditoriaServiceImpl.class})
class ResenaAdminIntegracionTest {

    @Autowired TestEntityManager em;
    @Autowired ResenaAdminService service;
    @Autowired ResenaRepository resenaRepository;
    @Autowired AuditoriaAdminRepository auditoriaRepository;

    private UsuarioActual admin;
    private Destino valle;
    private Destino tayrona;
    private Resena buenaConFoto;
    private Resena mala;

    @BeforeEach
    void setUp() {
        admin = new UsuarioActual(usuario("admin@sendera.com", "Ada", "Admin", "ROLE_ADMIN").getId(), true);
        Usuario ana = usuario("ana@correo.com", "Ana", "Pérez", "ROLE_CLIENTE");
        Usuario luis = usuario("luis@correo.com", "Luis", "Gómez", "ROLE_CLIENTE");
        valle = destino("Valle de Cocora");
        tayrona = destino("Tayrona");

        buenaConFoto = resena(valle, ana, "Un paisaje hermoso y muy tranquilo", 5, new byte[]{1, 2, 3});
        mala = resena(tayrona, luis, "Estaba sucio y 100% decepcionante", 1, null);
        resena(tayrona, ana, "Normal, nada especial", 3, null);
        em.flush();
        em.clear();
    }

    private Usuario usuario(String correo, String nombre, String apellidos, String rol) {
        return em.persist(Usuario.builder().nombre(nombre).apellidos(apellidos).correo(correo).contrasenaHash("hash").rol(rol).build());
    }

    private Destino destino(String nombre) {
        return em.persist(Destino.builder().nombre(nombre).descripcion("Paisaje").ubicacion("Colombia").dificultad("BAJA")
                .accesibilidad(true).precioBase(new BigDecimal("85000.00")).fotosUrl(new ArrayList<>()).build());
    }

    private Resena resena(Destino d, Usuario u, String comentario, int calificacion, byte[] imagen) {
        return em.persist(Resena.builder().destino(d).usuario(u).comentario(comentario).calificacion(calificacion)
                .imagen(imagen).imagenContentType(imagen == null ? null : "image/png").build());
    }

    @Test
    void listaTodasLasResenasIndicandoSiTienenImagen() {
        PaginaDto<ResenaAdminDto> pagina = service.listar(null, null, null, 0, 20);

        assertEquals(3, pagina.getTotal());
        ResenaAdminDto conFoto = pagina.getContenido().stream().filter(r -> r.getId().equals(buenaConFoto.getId())).findFirst().orElseThrow();
        assertTrue(conFoto.isTieneImagen());
        assertEquals("Ana Pérez", conFoto.getAutor());
        assertEquals("Valle de Cocora", conFoto.getDestinoNombre());
        assertFalse(pagina.getContenido().stream().filter(r -> r.getId().equals(mala.getId())).findFirst().orElseThrow().isTieneImagen());
    }

    @Test
    void filtraPorDestinoYPorCalificacionMaxima() {
        assertEquals(1, service.listar(valle.getId(), null, null, 0, 20).getTotal());
        assertEquals(2, service.listar(tayrona.getId(), null, null, 0, 20).getTotal());
        assertEquals(2, service.listar(null, 3, null, 0, 20).getTotal());
        assertEquals(1, service.listar(null, 1, null, 0, 20).getTotal());
        assertEquals(1, service.listar(tayrona.getId(), 1, null, 0, 20).getTotal());
    }

    @Test
    void buscaEnElComentarioOEnElAutorYTrataLosComodinesComoTexto() {
        assertEquals(1, service.listar(null, null, "HERMOSO", 0, 20).getTotal());
        assertEquals(2, service.listar(null, null, "ana", 0, 20).getTotal());
        assertEquals(1, service.listar(null, null, "100%", 0, 20).getTotal());
        assertEquals(0, service.listar(null, null, "%zzz", 0, 20).getTotal());
    }

    @Test
    void pagina() {
        assertEquals(2, service.listar(null, null, null, 0, 2).getContenido().size());
        assertEquals(1, service.listar(null, null, null, 1, 2).getContenido().size());
    }

    @Test
    void validaLosParametros() {
        assertThrows(FiltroInvalidoException.class, () -> service.listar(null, 0, null, 0, 20));
        assertThrows(FiltroInvalidoException.class, () -> service.listar(null, 6, null, 0, 20));
        assertThrows(FiltroInvalidoException.class, () -> service.listar(null, null, null, -1, 20));
        assertThrows(FiltroInvalidoException.class, () -> service.listar(null, null, null, 0, 101));
    }

    @Test
    void eliminarBorraLaResenaYDejaAuditoriaSinElTexto() {
        service.eliminar(mala.getId(), admin);
        em.flush();
        em.clear();

        assertTrue(resenaRepository.findById(mala.getId()).isEmpty());
        assertEquals(2, resenaRepository.count());
        List<AuditoriaAdmin> registros = auditoriaRepository.findAll();
        assertEquals(1, registros.size());
        assertEquals("RESENA_ELIMINAR", registros.get(0).getAccion());
        assertEquals(admin.id(), registros.get(0).getAdminId());
        assertEquals("destino=" + tayrona.getId() + ", autor=" + mala.getUsuario().getId(), registros.get(0).getDetalle());
        assertFalse(registros.get(0).getDetalle().contains("decepcionante"));
    }

    @Test
    void eliminarUnaResenaInexistenteDevuelveNoEncontradoYNoAudita() {
        assertThrows(RecursoNoEncontradoException.class, () -> service.eliminar(9999L, admin));
        assertEquals(0, auditoriaRepository.count());
    }
}
