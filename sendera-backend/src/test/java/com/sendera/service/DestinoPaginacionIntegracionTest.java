package com.sendera.service;

import com.sendera.dto.DestinoDto;
import com.sendera.dto.FiltroDestinoDto;
import com.sendera.dto.PaginaResultadosDto;
import com.sendera.exception.FiltroInvalidoException;
import com.sendera.model.Destino;
import com.sendera.service.impl.AuditoriaServiceImpl;
import com.sendera.service.impl.DestinoServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** OB-01: GET /destinos pagina en la base de datos (H2), conserva filtros y orden y calcula los totales. */
@DataJpaTest
@ActiveProfiles("test")
@Import({DestinoServiceImpl.class, AuditoriaServiceImpl.class})
class DestinoPaginacionIntegracionTest {

    @Autowired TestEntityManager em;
    @Autowired DestinoService service;

    private void crear(int cantidad) {
        for (int i = 1; i <= cantidad; i++) {
            em.persist(destino(String.format("Destino %02d", i), i % 2 == 0 ? "Salento" : "Cartagena",
                    i % 3 == 0 ? "ALTA" : "BAJA", i % 2 == 0, 100000 + i * 1000));
        }
        em.flush();
        em.clear();
    }

    private Destino destino(String nombre, String ubicacion, String dificultad, boolean accesible, int precio) {
        return Destino.builder().nombre(nombre).ubicacion(ubicacion).descripcion("Descripción").dificultad(dificultad)
                .accesibilidad(accesible).precioBase(new BigDecimal(precio)).fotosUrl(List.of()).build();
    }

    private static List<String> nombres(PaginaResultadosDto<DestinoDto> pagina) {
        return pagina.getContenido().stream().map(DestinoDto::getNombre).toList();
    }

    @Test
    void laPagina0YLa1DevuelvenSusDestinosEnOrdenPorId() {
        crear(25);

        PaginaResultadosDto<DestinoDto> primera = service.listarDestinos(new FiltroDestinoDto(), 0, 10);
        PaginaResultadosDto<DestinoDto> segunda = service.listarDestinos(new FiltroDestinoDto(), 1, 10);

        assertEquals(10, primera.getContenido().size());
        assertEquals("Destino 01", nombres(primera).get(0));
        assertEquals("Destino 10", nombres(primera).get(9));
        assertEquals("Destino 11", nombres(segunda).get(0));
        assertEquals("Destino 20", nombres(segunda).get(9));
        assertEquals(0, primera.getPagina());
        assertEquals(1, segunda.getPagina());
        assertEquals(10, segunda.getTamano());
    }

    @Test
    void totalElementosYTotalPaginasSonCorrectosYLaUltimaPaginaVieneIncompleta() {
        crear(25);

        PaginaResultadosDto<DestinoDto> ultima = service.listarDestinos(new FiltroDestinoDto(), 2, 10);

        assertEquals(25, ultima.getTotalElementos());
        assertEquals(3, ultima.getTotalPaginas());
        assertEquals(5, ultima.getContenido().size());
        assertEquals("Destino 25", nombres(ultima).get(4));
    }

    @Test
    void unaPaginaMasAllaDelFinalVieneVaciaConLosTotalesReales() {
        crear(5);

        PaginaResultadosDto<DestinoDto> pagina = service.listarDestinos(new FiltroDestinoDto(), 4, 10);

        assertTrue(pagina.getContenido().isEmpty());
        assertEquals(5, pagina.getTotalElementos());
        assertEquals(1, pagina.getTotalPaginas());
    }

    @Test
    void sinDestinosHayCeroPaginas() {
        PaginaResultadosDto<DestinoDto> pagina = service.listarDestinos(new FiltroDestinoDto(), 0, 12);

        assertTrue(pagina.getContenido().isEmpty());
        assertEquals(0, pagina.getTotalElementos());
        assertEquals(0, pagina.getTotalPaginas());
    }

    @Test
    void unTamanoMayorQue50SeLimitaA50() {
        crear(55);

        PaginaResultadosDto<DestinoDto> pagina = service.listarDestinos(new FiltroDestinoDto(), 0, 1000);

        assertEquals(50, pagina.getTamano());
        assertEquals(50, pagina.getContenido().size());
        assertEquals(55, pagina.getTotalElementos());
        assertEquals(2, pagina.getTotalPaginas());
    }

    @Test
    void tamanoCeroYPaginaNegativaSeRechazan() {
        FiltroInvalidoException tamano = assertThrows(FiltroInvalidoException.class,
                () -> service.listarDestinos(new FiltroDestinoDto(), 0, 0));
        assertEquals("El tamaño de página debe ser al menos 1.", tamano.getMessage());
        FiltroInvalidoException pagina = assertThrows(FiltroInvalidoException.class,
                () -> service.listarDestinos(new FiltroDestinoDto(), -1, 12));
        assertEquals("El número de página debe ser 0 o mayor.", pagina.getMessage());
    }

    @Test
    void losFiltrosCombinadosSeAplicanAntesDePaginar() {
        crear(30);
        // Accesibles (pares) en Salento (pares) con dificultad ALTA (múltiplos de 3) -> múltiplos de 6: 6, 12, 18, 24, 30.
        FiltroDestinoDto filtro = FiltroDestinoDto.builder().accesibilidad(true).ubicacion("salento").dificultad("alta").build();

        PaginaResultadosDto<DestinoDto> primera = service.listarDestinos(filtro, 0, 2);
        PaginaResultadosDto<DestinoDto> tercera = service.listarDestinos(filtro, 2, 2);

        assertEquals(5, primera.getTotalElementos());
        assertEquals(3, primera.getTotalPaginas());
        assertEquals(List.of("Destino 06", "Destino 12"), nombres(primera));
        assertEquals(List.of("Destino 30"), nombres(tercera));
    }

    @Test
    void elFiltroDePrecioYElDeTextoTambienSePaginan() {
        crear(30);
        FiltroDestinoDto filtro = FiltroDestinoDto.builder().precioMin(new BigDecimal("110000")).precioMax(new BigDecimal("120000"))
                .q("cartagena").build();

        PaginaResultadosDto<DestinoDto> pagina = service.listarDestinos(filtro, 0, 3);

        // Impares entre 110.000 (i=10) y 120.000 (i=20): 11, 13, 15, 17, 19.
        assertEquals(5, pagina.getTotalElementos());
        assertEquals(List.of("Destino 11", "Destino 13", "Destino 15"), nombres(pagina));
    }

    @Test
    void nombreYUbicacionFiltranPorTextoContenidoSinImportarMayusculas() {
        crear(12);

        assertEquals(1, service.listarDestinos(FiltroDestinoDto.builder().nombre("DESTINO 07").build(), 0, 12).getTotalElementos());
        assertEquals(6, service.listarDestinos(FiltroDestinoDto.builder().ubicacion("cartag").build(), 0, 12).getTotalElementos());
        assertEquals(0, service.listarDestinos(FiltroDestinoDto.builder().nombre("destino 07").ubicacion("salento").build(), 0, 12)
                .getTotalElementos());
    }
}
