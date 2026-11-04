package com.sendera.repository;

import com.sendera.model.Destino;
import com.sendera.model.Itinerario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** RF-011/RF-012: solo se ofrecen itinerarios con cupos y que no han terminado (contra H2). */
@DataJpaTest
@ActiveProfiles("test")
class ItinerarioDisponibleIntegracionTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 4);

    @Autowired TestEntityManager em;
    @Autowired ItinerarioRepository repository;

    private Destino destino;
    private Destino otroDestino;

    @BeforeEach
    void datos() {
        destino = em.persist(destino("Valle"));
        otroDestino = em.persist(destino("Playa"));
        itinerario(destino, "Futuro lejano", HOY.plusDays(30), HOY.plusDays(32), 4);
        itinerario(destino, "Futuro cercano", HOY.plusDays(5), HOY.plusDays(6), 1);
        itinerario(destino, "Termina hoy", HOY.minusDays(1), HOY, 3);
        itinerario(destino, "Ya terminó", HOY.minusDays(10), HOY.minusDays(1), 5);
        itinerario(destino, "Sin cupos", HOY.plusDays(2), HOY.plusDays(3), 0);
        itinerario(otroDestino, "De otro destino", HOY.plusDays(2), HOY.plusDays(3), 9);
        em.flush();
    }

    private Destino destino(String nombre) {
        return Destino.builder().nombre(nombre).descripcion("Paisaje").ubicacion("Colombia").dificultad("BAJA")
                .accesibilidad(true).precioBase(new BigDecimal("85000.00")).fotosUrl(new ArrayList<>()).build();
    }

    private void itinerario(Destino d, String nombre, LocalDate inicio, LocalDate fin, int cupos) {
        em.persist(Itinerario.builder().destino(d).nombre(nombre).descripcion("Ruta").fechaInicio(inicio)
                .fechaFin(fin).precioBase(new BigDecimal("195000.00")).cuposDisponibles(cupos).build());
    }

    private List<String> nombresDisponibles(Long destinoId) {
        return repository.findByDestinoIdAndCuposDisponiblesGreaterThanAndFechaFinGreaterThanEqualOrderByFechaInicioAsc(
                destinoId, 0, HOY).stream().map(Itinerario::getNombre).toList();
    }

    @Test
    void devuelveSoloConCuposYQueNoHanTerminadoOrdenadosPorFechaDeInicio() {
        // "Termina hoy" cuenta (fechaFin = hoy); "Sin cupos" y "Ya terminó" no; el otro destino tampoco.
        assertEquals(List.of("Termina hoy", "Futuro cercano", "Futuro lejano"), nombresDisponibles(destino.getId()));
    }

    @Test
    void unDestinoSinItinerariosDisponiblesDevuelveListaVacia() {
        Destino vacio = em.persist(destino("Vacío"));
        assertEquals(List.of(), nombresDisponibles(vacio.getId()));
        assertEquals(List.of("De otro destino"), nombresDisponibles(otroDestino.getId()));
    }
}
