package com.sendera.service;

import com.sendera.dto.CrearReservaDto;
import com.sendera.dto.ReservaDto;
import com.sendera.exception.RecursoNoEncontradoException;
import com.sendera.model.Destino;
import com.sendera.model.Itinerario;
import com.sendera.model.Usuario;
import com.sendera.security.UsuarioActual;
import com.sendera.service.impl.ReservaServiceImpl;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** RF-005/RF-012: propiedad de las reservas contra una base H2 real (repositorios y servicio sin mocks). */
@DataJpaTest
@ActiveProfiles("test")
@Import(ReservaServiceImpl.class)
class ReservaPropiedadIntegracionTest {

    @Autowired
    TestEntityManager em;
    @Autowired
    ReservaService reservaService;

    private UsuarioActual ana;
    private UsuarioActual luis;
    private UsuarioActual admin;
    private Long itinerarioId;

    @BeforeEach
    void setUp() {
        ana = new UsuarioActual(crearUsuario("ana@correo.com", "ROLE_CLIENTE").getId(), false);
        luis = new UsuarioActual(crearUsuario("luis@correo.com", "ROLE_CLIENTE").getId(), false);
        admin = new UsuarioActual(crearUsuario("admin@correo.com", "ROLE_ADMIN").getId(), true);

        Destino destino = em.persist(Destino.builder().nombre("Valle").descripcion("Paisaje").ubicacion("Salento")
                .dificultad("BAJA").accesibilidad(true).precioBase(new BigDecimal("85000.00"))
                .fotosUrl(new ArrayList<>()).build());
        itinerarioId = em.persist(Itinerario.builder().destino(destino).nombre("Cocora").descripcion("Ruta")
                .fechaInicio(LocalDate.of(2026, 11, 18)).fechaFin(LocalDate.of(2026, 11, 19))
                .precioBase(new BigDecimal("195000.00")).cuposDisponibles(5).build()).getId();
        em.flush();
    }

    private Usuario crearUsuario(String correo, String rol) {
        return em.persist(Usuario.builder().nombre("N").apellidos("A").correo(correo)
                .contrasenaHash("hash").rol(rol).build());
    }

    private ReservaDto reservaDe(UsuarioActual usuario) {
        CrearReservaDto dto = new CrearReservaDto();
        dto.setItinerarioId(itinerarioId);
        return reservaService.crear(dto, usuario);
    }

    @Test
    void elPropietarioConsultaYCancelaSuReserva() {
        ReservaDto creada = reservaDe(ana);

        assertEquals("PENDIENTE", creada.getEstadoReserva());
        assertEquals(creada.getId(), reservaService.obtenerPorId(creada.getId(), ana).getId());
        assertEquals("CANCELADA", reservaService.cancelar(creada.getId(), ana).getEstadoReserva());
    }

    @Test
    void otroUsuarioNoPuedeConsultarNiCancelarReservaAjena() {
        Long id = reservaDe(ana).getId();

        assertThrows(RecursoNoEncontradoException.class, () -> reservaService.obtenerPorId(id, luis));
        assertThrows(RecursoNoEncontradoException.class, () -> reservaService.cancelar(id, luis));
        // La reserva de Ana sigue intacta
        assertEquals("PENDIENTE", reservaService.obtenerPorId(id, ana).getEstadoReserva());
    }

    @Test
    void cadaUsuarioListaSoloSusReservasYAdminListaTodas() {
        reservaDe(ana);
        reservaDe(ana);
        reservaDe(luis);

        assertEquals(2, reservaService.listar(ana).size());
        assertEquals(1, reservaService.listar(luis).size());
        assertEquals(3, reservaService.listar(admin).size());
    }

    @Test
    void adminPuedeConsultarReservaDeOtroUsuario() {
        Long id = reservaDe(ana).getId();

        assertEquals(id, reservaService.obtenerPorId(id, admin).getId());
    }

    @Test
    void usuarioSinReservasObtieneListaVacia() {
        List<ReservaDto> reservas = reservaService.listar(luis);

        assertTrue(reservas.isEmpty());
    }
}
