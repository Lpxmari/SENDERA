package com.sendera.service;

import com.sendera.dto.CrearResenaDto;
import com.sendera.exception.ResenaDuplicadaException;
import com.sendera.exception.ResenaNoPermitidaException;
import com.sendera.model.Destino;
import com.sendera.model.EstadoReserva;
import com.sendera.model.Itinerario;
import com.sendera.model.Resena;
import com.sendera.model.Reserva;
import com.sendera.model.Usuario;
import com.sendera.repository.ResenaRepository;
import com.sendera.security.UsuarioActual;
import com.sendera.service.impl.ResenaServiceImpl;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Reglas de reseñas contra H2: reserva CONFIRMADA del destino y una única reseña por persona y destino. */
@DataJpaTest
@ActiveProfiles("test")
@Import(ResenaServiceImpl.class)
class ResenaReglasIntegracionTest {

    @Autowired TestEntityManager em;
    @Autowired ResenaService resenaService;
    @Autowired ResenaRepository resenaRepository;

    private Usuario ana;
    private Usuario luis;
    private Usuario admin;
    private Destino valle;
    private Destino playa;
    private Itinerario itinerarioValle;
    private Itinerario itinerarioPlaya;

    @BeforeEach
    void setUp() {
        ana = usuario("ana@correo.com", "ROLE_CLIENTE");
        luis = usuario("luis@correo.com", "ROLE_CLIENTE");
        admin = usuario("admin@correo.com", "ROLE_ADMIN");
        valle = destino("Valle");
        playa = destino("Playa");
        itinerarioValle = itinerario(valle);
        itinerarioPlaya = itinerario(playa);
        em.flush();
    }

    private Usuario usuario(String correo, String rol) {
        return em.persist(Usuario.builder().nombre("N").apellidos("A").correo(correo)
                .contrasenaHash("hash").rol(rol).build());
    }

    private Destino destino(String nombre) {
        return em.persist(Destino.builder().nombre(nombre).descripcion("Paisaje").ubicacion("Colombia")
                .dificultad("BAJA").accesibilidad(true).precioBase(new BigDecimal("85000.00"))
                .fotosUrl(new ArrayList<>()).build());
    }

    private Itinerario itinerario(Destino destino) {
        return em.persist(Itinerario.builder().destino(destino).nombre("Ruta").descripcion("Ruta")
                .fechaInicio(LocalDate.of(2026, 11, 18)).fechaFin(LocalDate.of(2026, 11, 19))
                .precioBase(new BigDecimal("195000.00")).cuposDisponibles(5).build());
    }

    private void reserva(Usuario usuario, Itinerario itinerario, EstadoReserva estado) {
        em.persist(Reserva.builder().usuario(usuario).itinerario(itinerario).fechaReserva(LocalDate.of(2026, 11, 18))
                .montoTotal(new BigDecimal("195000.00")).estadoReserva(estado).build());
        em.flush();
    }

    private CrearResenaDto dto() {
        CrearResenaDto dto = new CrearResenaDto();
        dto.setComentario("Muy buena experiencia");
        dto.setCalificacion(5);
        return dto;
    }

    private UsuarioActual actual(Usuario usuario, boolean esAdmin) {
        return new UsuarioActual(usuario.getId(), esAdmin);
    }

    @Test
    void conReservaConfirmadaDelDestinoPuedeReseñar() {
        reserva(ana, itinerarioValle, EstadoReserva.CONFIRMADA);

        assertEquals("Muy buena experiencia", resenaService.crear(valle.getId(), dto(), null, actual(ana, false)).getComentario());
        assertEquals(1, resenaRepository.count());
    }

    @Test
    void sinReservaONoConfirmadaONoDeEseDestinoRecibeProhibido() {
        reserva(ana, itinerarioValle, EstadoReserva.PENDIENTE);
        reserva(ana, itinerarioPlaya, EstadoReserva.CONFIRMADA); // confirmada, pero de otro destino
        reserva(luis, itinerarioValle, EstadoReserva.CANCELADA);

        assertThrows(ResenaNoPermitidaException.class,
                () -> resenaService.crear(valle.getId(), dto(), null, actual(ana, false))); // pendiente
        assertThrows(ResenaNoPermitidaException.class,
                () -> resenaService.crear(valle.getId(), dto(), null, actual(luis, false))); // cancelada
        assertEquals(0, resenaRepository.count());
    }

    @Test
    void laReservaConfirmadaDeOtraPersonaNoHabilita() {
        reserva(ana, itinerarioValle, EstadoReserva.CONFIRMADA);

        assertThrows(ResenaNoPermitidaException.class,
                () -> resenaService.crear(valle.getId(), dto(), null, actual(luis, false)));
    }

    @Test
    void elAdministradorSinReservaConfirmadaRecibeProhibido() {
        assertThrows(ResenaNoPermitidaException.class,
                () -> resenaService.crear(valle.getId(), dto(), null, actual(admin, true)));
    }

    @Test
    void unaSegundaReseñaDelMismoDestinoRecibeConflicto() {
        reserva(ana, itinerarioValle, EstadoReserva.CONFIRMADA);
        resenaService.crear(valle.getId(), dto(), null, actual(ana, false));

        assertThrows(ResenaDuplicadaException.class,
                () -> resenaService.crear(valle.getId(), dto(), null, actual(ana, false)));
        assertEquals(1, resenaRepository.count());
    }

    @Test
    void personasDistintasPuedenReseñarElMismoDestinoYUnaPersonaDestinosDistintos() {
        reserva(ana, itinerarioValle, EstadoReserva.CONFIRMADA);
        reserva(ana, itinerarioPlaya, EstadoReserva.CONFIRMADA);
        reserva(luis, itinerarioValle, EstadoReserva.CONFIRMADA);

        resenaService.crear(valle.getId(), dto(), null, actual(ana, false));
        resenaService.crear(playa.getId(), dto(), null, actual(ana, false));
        resenaService.crear(valle.getId(), dto(), null, actual(luis, false));

        assertEquals(3, resenaRepository.count());
    }

    @Test
    void laBaseRechazaDosReseñasDeLaMismaPersonaParaElMismoDestino() {
        em.persist(Resena.builder().destino(valle).usuario(ana).comentario("Una").calificacion(4).build());
        em.flush();

        // Con IDENTITY el INSERT se ejecuta en persist(), así que la violación puede saltar ahí o en flush().
        assertThrows(Exception.class, () -> {
            em.persist(Resena.builder().destino(valle).usuario(ana).comentario("Dos").calificacion(5).build());
            em.flush();
        });
    }
}
