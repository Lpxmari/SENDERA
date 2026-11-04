package com.sendera.repository;

import com.sendera.model.Actividad;
import com.sendera.model.Destino;
import com.sendera.model.RangoDuracion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** RNF-26 / RF-008: filtro de actividades por rango de duración, con límites inclusivos (contra H2). */
@DataJpaTest
@ActiveProfiles("test")
class ActividadDuracionIntegracionTest {

    @Autowired TestEntityManager em;
    @Autowired ActividadRepository repository;

    private Destino valle;
    private Destino playa;

    @BeforeEach
    void datos() {
        valle = em.persist(destino("Valle"));
        playa = em.persist(destino("Playa"));
        actividad(valle, "A-60", 60);
        actividad(valle, "B-61", 61);
        actividad(valle, "C-180", 180);
        actividad(valle, "D-181", 181);
        actividad(playa, "E-30", 30);
        actividad(playa, "F-300", 300);
        em.flush();
    }

    private Destino destino(String nombre) {
        return Destino.builder().nombre(nombre).descripcion("Paisaje").ubicacion("Colombia").dificultad("BAJA")
                .accesibilidad(true).precioBase(new BigDecimal("85000.00")).fotosUrl(new ArrayList<>()).build();
    }

    private void actividad(Destino d, String nombre, int minutos) {
        em.persist(Actividad.builder().destino(d).nombre(nombre).descripcion("Actividad").duracionMinutos(minutos)
                .dificultad("BAJA").precioPorPersona(new BigDecimal("50000.00")).accesibilidad(true).build());
    }

    private List<String> buscar(Long destinoId, RangoDuracion rango) {
        Integer min = rango == null ? null : rango.getMinMinutos();
        Integer max = rango == null ? null : rango.getMaxMinutos();
        return repository.buscar(destinoId, min, max).stream().map(Actividad::getNombre).toList();
    }

    @Test
    void sinFiltrosDevuelveTodasOrdenadasPorNombre() {
        assertEquals(List.of("A-60", "B-61", "C-180", "D-181", "E-30", "F-300"), buscar(null, null));
    }

    @Test
    void hastaSesentaIncluyeElLimite() {
        assertEquals(List.of("A-60", "E-30"), buscar(null, RangoDuracion.HASTA_60_MIN));
    }

    @Test
    void de61a180IncluyeAmbosLimites() {
        assertEquals(List.of("B-61", "C-180"), buscar(null, RangoDuracion.DE_61_A_180_MIN));
    }

    @Test
    void masDe180ExcluyeElLimiteYAdmiteDuracionesLargas() {
        assertEquals(List.of("D-181", "F-300"), buscar(null, RangoDuracion.MAS_DE_180_MIN));
    }

    @Test
    void sePuedeCombinarConElDestino() {
        assertEquals(List.of("D-181"), buscar(valle.getId(), RangoDuracion.MAS_DE_180_MIN));
        assertEquals(List.of("E-30"), buscar(playa.getId(), RangoDuracion.HASTA_60_MIN));
        assertEquals(List.of(), buscar(playa.getId(), RangoDuracion.DE_61_A_180_MIN));
    }

    @Test
    void soloDestinoSinDuracion() {
        assertEquals(List.of("E-30", "F-300"), buscar(playa.getId(), null));
    }
}
