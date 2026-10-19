package com.sendera.repository;

import com.sendera.dto.FiltroDestinoDto;
import com.sendera.model.Destino;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** RF-008: los filtros de búsqueda se resuelven en la base de datos (H2), no en memoria. */
@DataJpaTest
@ActiveProfiles("test")
class DestinoSpecificationsIntegracionTest {

    @Autowired DestinoRepository repository;

    @BeforeEach
    void datos() {
        repository.save(destino("Valle de Cocora", "Salento", "Palmas de cera entre montañas", "ALTA", false, "300000"));
        repository.save(destino("Playa Dorada", "Santa Marta", "Descanso frente al mar", "BAJA", true, "150000"));
        repository.save(destino("Centro Histórico", "Cartagena", "Calles coloniales, 100% caminable_", "BAJA", true, "200000"));
    }

    private Destino destino(String nombre, String ubicacion, String descripcion, String dificultad, boolean accesible, String precio) {
        return Destino.builder().nombre(nombre).ubicacion(ubicacion).descripcion(descripcion).dificultad(dificultad)
                .accesibilidad(accesible).precioBase(new BigDecimal(precio)).fotosUrl(List.of()).build();
    }

    private List<String> nombres(FiltroDestinoDto filtro) {
        return repository.findAll(DestinoSpecifications.conFiltros(filtro)).stream().map(Destino::getNombre).sorted().toList();
    }

    @Test
    void sinFiltrosDevuelveTodos() {
        assertEquals(3, nombres(new FiltroDestinoDto()).size());
    }

    @Test
    void textoBuscaSinImportarMayusculasEnNombreUbicacionYDescripcion() {
        assertEquals(List.of("Valle de Cocora"), nombres(FiltroDestinoDto.builder().q("COCORA").build()));
        assertEquals(List.of("Playa Dorada"), nombres(FiltroDestinoDto.builder().q("santa marta").build()));
        assertEquals(List.of("Playa Dorada"), nombres(FiltroDestinoDto.builder().q("frente al mar").build()));
        assertEquals(List.of(), nombres(FiltroDestinoDto.builder().q("inexistente").build()));
    }

    @Test
    void textoEnBlancoSeIgnora() {
        assertEquals(3, nombres(FiltroDestinoDto.builder().q("   ").build()).size());
    }

    @Test
    void loscaracteresComodinDelTextoSeTratanComoLiterales() {
        // Solo "Centro Histórico" contiene un '%' y un '_' reales; si fueran comodines coincidirían todos.
        assertEquals(List.of("Centro Histórico"), nombres(FiltroDestinoDto.builder().q("%").build()));
        assertEquals(List.of("Centro Histórico"), nombres(FiltroDestinoDto.builder().q("100%").build()));
        assertEquals(List.of("Centro Histórico"), nombres(FiltroDestinoDto.builder().q("_").build()));
        assertEquals(List.of("Centro Histórico"), nombres(FiltroDestinoDto.builder().q("caminable_").build()));
    }

    @Test
    void rangoDePrecioIncluyeLosExtremos() {
        assertEquals(List.of("Centro Histórico", "Playa Dorada"),
                nombres(FiltroDestinoDto.builder().precioMin(new BigDecimal("150000")).precioMax(new BigDecimal("200000")).build()));
        assertEquals(List.of("Valle de Cocora"), nombres(FiltroDestinoDto.builder().precioMin(new BigDecimal("250000")).build()));
        assertEquals(List.of("Playa Dorada"), nombres(FiltroDestinoDto.builder().precioMax(new BigDecimal("150000")).build()));
    }

    @Test
    void filtraPorDificultadYAccesibilidad() {
        assertEquals(List.of("Valle de Cocora"), nombres(FiltroDestinoDto.builder().dificultad("ALTA").build()));
        assertEquals(List.of("Centro Histórico", "Playa Dorada"), nombres(FiltroDestinoDto.builder().accesibilidad(true).build()));
        assertEquals(List.of("Valle de Cocora"), nombres(FiltroDestinoDto.builder().accesibilidad(false).build()));
        assertEquals(List.of(), nombres(FiltroDestinoDto.builder().dificultad("ALTA").accesibilidad(true).build()));
        assertEquals(3, nombres(FiltroDestinoDto.builder().dificultad(" ").build()).size());
    }

    @Test
    void losFiltrosSeCombinanConY() {
        assertEquals(List.of("Playa Dorada"),
                nombres(FiltroDestinoDto.builder().q("playa").dificultad("BAJA").accesibilidad(true)
                        .precioMax(new BigDecimal("160000")).build()));
    }
}
