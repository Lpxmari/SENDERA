package com.sendera.service;

import com.sendera.dto.CambiarEstadoReservaDto;
import com.sendera.dto.PaginaDto;
import com.sendera.dto.ReservaAdminDto;
import com.sendera.exception.FiltroInvalidoException;
import com.sendera.exception.RecursoNoEncontradoException;
import com.sendera.exception.ReservaNoPermitidaException;
import com.sendera.model.AuditoriaAdmin;
import com.sendera.model.Destino;
import com.sendera.model.EstadoReserva;
import com.sendera.model.Itinerario;
import com.sendera.model.Reserva;
import com.sendera.model.Usuario;
import com.sendera.repository.AuditoriaAdminRepository;
import com.sendera.security.UsuarioActual;
import com.sendera.service.impl.AuditoriaServiceImpl;
import com.sendera.service.impl.ReservaAdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** RF-012/RF-013 contra H2: filtros reales, transiciones de estado, cupos y auditoría persistida. */
@DataJpaTest
@ActiveProfiles("test")
@Import({ReservaAdminServiceImpl.class, AuditoriaServiceImpl.class})
class ReservaAdminIntegracionTest {

    @Autowired TestEntityManager em;
    @Autowired ReservaAdminService service;
    @Autowired AuditoriaAdminRepository auditoriaRepository;

    private UsuarioActual admin;
    private Itinerario cocora;
    private Reserva pendienteAna;
    private Reserva confirmadaLuis;
    private Reserva canceladaAna;

    @BeforeEach
    void setUp() {
        admin = new UsuarioActual(usuario("admin@sendera.com", "Ada", "Admin", "ROLE_ADMIN").getId(), true);
        Usuario ana = usuario("ana@correo.com", "Ana", "Pérez", "ROLE_CLIENTE");
        Usuario luis = usuario("luis@correo.com", "Luis", "100%_Gómez", "ROLE_CLIENTE");

        Destino valle = destino("Valle de Cocora");
        Destino tayrona = destino("Tayrona");
        cocora = itinerario(valle, "Cocora en dos días", 5);
        Itinerario playa = itinerario(tayrona, "Playas del Tayrona", 8);

        pendienteAna = reserva(ana, cocora, "2026-10-10", EstadoReserva.PENDIENTE);
        confirmadaLuis = reserva(luis, playa, "2026-10-12", EstadoReserva.CONFIRMADA);
        canceladaAna = reserva(ana, playa, "2026-10-14", EstadoReserva.CANCELADA);
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

    private Itinerario itinerario(Destino destino, String nombre, int cupos) {
        return em.persist(Itinerario.builder().destino(destino).nombre(nombre).descripcion("Ruta")
                .fechaInicio(LocalDate.of(2026, 11, 18)).fechaFin(LocalDate.of(2026, 11, 19))
                .precioBase(new BigDecimal("195000.00")).cuposDisponibles(cupos).build());
    }

    private Reserva reserva(Usuario u, Itinerario i, String fecha, EstadoReserva estado) {
        return em.persist(Reserva.builder().usuario(u).itinerario(i).fechaReserva(LocalDate.parse(fecha))
                .montoTotal(new BigDecimal("195000.00")).estadoReserva(estado).build());
    }

    private CambiarEstadoReservaDto estado(String valor) {
        CambiarEstadoReservaDto dto = new CambiarEstadoReservaDto();
        dto.setEstado(valor);
        return dto;
    }

    @Test
    void listaTodasLasReservasDeTodosLosUsuariosDeLaMasRecienteALaMasAntigua() {
        PaginaDto<ReservaAdminDto> pagina = service.listar(null, null, null, null, 0, 20);

        assertEquals(3, pagina.getTotal());
        assertEquals(canceladaAna.getId(), pagina.getContenido().get(0).getId());
        ReservaAdminDto primera = pagina.getContenido().get(2);
        assertEquals("Ana Pérez", primera.getUsuarioNombre());
        assertEquals("ana@correo.com", primera.getUsuarioCorreo());
        assertEquals("Cocora en dos días", primera.getItinerarioNombre());
        assertEquals("Valle de Cocora", primera.getDestinoNombre());
    }

    @Test
    void filtraPorEstado() {
        assertEquals(1, service.listar("confirmada", null, null, null, 0, 20).getTotal());
        assertEquals(1, service.listar("PENDIENTE", null, null, null, 0, 20).getTotal());
        assertEquals(confirmadaLuis.getId(), service.listar("CONFIRMADA", null, null, null, 0, 20).getContenido().get(0).getId());
    }

    @Test
    void filtraPorRangoDeFechas() {
        assertEquals(2, service.listar(null, null, LocalDate.of(2026, 10, 11), null, 0, 20).getTotal());
        assertEquals(2, service.listar(null, null, null, LocalDate.of(2026, 10, 12), 0, 20).getTotal());
        assertEquals(1, service.listar(null, null, LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 12), 0, 20).getTotal());
    }

    @Test
    void buscaPorUsuarioCorreoDestinoOItinerario() {
        assertEquals(2, service.listar(null, "ana", null, null, 0, 20).getTotal());
        assertEquals(2, service.listar(null, "TAYRONA", null, null, 0, 20).getTotal());
        assertEquals(2, service.listar(null, "tayrona", null, null, 0, 20).getTotal());
        assertEquals(1, service.listar(null, "dos días", null, null, 0, 20).getTotal());
        assertEquals(1, service.listar(null, "luis@correo", null, null, 0, 20).getTotal());
    }

    @Test
    void losComodinesDeLaBusquedaSeTratanComoTextoLiteral() {
        assertEquals(1, service.listar(null, "100%_", null, null, 0, 20).getTotal());
        assertEquals(0, service.listar(null, "%zzz", null, null, 0, 20).getTotal());
    }

    @Test
    void combinaFiltrosYPagina() {
        assertEquals(1, service.listar("CANCELADA", "ana", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), 0, 20).getTotal());
        PaginaDto<ReservaAdminDto> primera = service.listar(null, null, null, null, 0, 2);
        assertEquals(2, primera.getContenido().size());
        assertEquals(1, service.listar(null, null, null, null, 1, 2).getContenido().size());
    }

    @Test
    void validaLosParametros() {
        assertThrows(FiltroInvalidoException.class, () -> service.listar("ENVIADA", null, null, null, 0, 20));
        assertThrows(FiltroInvalidoException.class, () -> service.listar(null, null, null, null, -1, 20));
        assertThrows(FiltroInvalidoException.class, () -> service.listar(null, null, null, null, 0, 101));
        assertThrows(FiltroInvalidoException.class,
                () -> service.listar(null, null, LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 1), 0, 20));
    }

    @Test
    void confirmarUnaReservaPendienteLaAudita() {
        ReservaAdminDto resultado = service.cambiarEstado(pendienteAna.getId(), estado("CONFIRMADA"), admin);
        em.flush();
        em.clear();

        assertEquals("CONFIRMADA", resultado.getEstadoReserva());
        assertEquals(5, em.find(Itinerario.class, cocora.getId()).getCuposDisponibles());
        List<AuditoriaAdmin> registros = auditoriaRepository.findAll();
        assertEquals(1, registros.size());
        assertEquals("RESERVA_CONFIRMAR", registros.get(0).getAccion());
        assertEquals("PENDIENTE -> CONFIRMADA", registros.get(0).getDetalle());
        assertEquals(pendienteAna.getId(), registros.get(0).getRecursoId());
    }

    @Test
    void cancelarDevuelveElCupoAlItinerario() {
        service.cambiarEstado(pendienteAna.getId(), estado("CANCELADA"), admin);
        em.flush();
        em.clear();

        assertEquals(6, em.find(Itinerario.class, cocora.getId()).getCuposDisponibles());
        assertEquals(EstadoReserva.CANCELADA, em.find(Reserva.class, pendienteAna.getId()).getEstadoReserva());
        assertEquals("RESERVA_CANCELAR", auditoriaRepository.findAll().get(0).getAccion());
    }

    @Test
    void unaReservaConfirmadaSePuedeCancelar() {
        service.cambiarEstado(confirmadaLuis.getId(), estado("CANCELADA"), admin);
        em.flush();
        em.clear();

        assertEquals(EstadoReserva.CANCELADA, em.find(Reserva.class, confirmadaLuis.getId()).getEstadoReserva());
    }

    @Test
    void unaReservaCanceladaEsFinalYNoDevuelveOtroCupo() {
        assertThrows(ReservaNoPermitidaException.class, () -> service.cambiarEstado(canceladaAna.getId(), estado("CONFIRMADA"), admin));
        assertThrows(ReservaNoPermitidaException.class, () -> service.cambiarEstado(canceladaAna.getId(), estado("CANCELADA"), admin));
        assertEquals(0, auditoriaRepository.count());
    }

    @Test
    void noSePuedeLlevarLaReservaAlEstadoQueYaTiene() {
        assertThrows(ReservaNoPermitidaException.class, () -> service.cambiarEstado(confirmadaLuis.getId(), estado("CONFIRMADA"), admin));
        assertEquals(0, auditoriaRepository.count());
    }

    @Test
    void reservaInexistenteDevuelveNoEncontrado() {
        assertThrows(RecursoNoEncontradoException.class, () -> service.obtener(9999L));
        assertThrows(RecursoNoEncontradoException.class, () -> service.cambiarEstado(9999L, estado("CANCELADA"), admin));
    }
}
