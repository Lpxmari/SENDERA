package com.sendera.service;

import com.sendera.dto.GuardarViajeDto;
import com.sendera.dto.ViajeActividadDto;
import com.sendera.dto.ViajeActividadEntradaDto;
import com.sendera.dto.ViajeDto;
import com.sendera.dto.ViajeResumenDto;
import com.sendera.exception.HorarioSolapadoException;
import com.sendera.exception.RecursoNoEncontradoException;
import com.sendera.exception.ViajeInvalidoException;
import com.sendera.model.Actividad;
import com.sendera.model.Destino;
import com.sendera.model.Usuario;
import com.sendera.model.Viaje;
import com.sendera.security.UsuarioActual;
import com.sendera.service.impl.ViajeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** RF-009/RF-010 contra H2: propiedad del viaje, fechas pasadas, solapes de horario y orden dentro del día. */
@DataJpaTest
@ActiveProfiles("test")
@Import(ViajeServiceImpl.class)
class ViajeServiceImplIntegracionTest {

    private static final LocalDate HOY = LocalDate.now(ZoneId.of("America/Bogota"));

    @Autowired TestEntityManager em;
    @Autowired ViajeService service;

    private UsuarioActual ana;
    private UsuarioActual luis;
    private UsuarioActual admin;
    private Actividad recorrido;   // 3 h
    private Actividad finca;       // 2 h
    private Actividad centro;      // 2 h
    private Actividad nocturna;    // 2 h

    @BeforeEach
    void preparar() {
        ana = new UsuarioActual(usuario("ana@correo.com", "ROLE_CLIENTE").getId(), false);
        luis = new UsuarioActual(usuario("luis@correo.com", "ROLE_CLIENTE").getId(), false);
        admin = new UsuarioActual(usuario("admin@correo.com", "ROLE_ADMIN").getId(), true);

        Destino filandia = em.persist(Destino.builder().nombre("Filandia").descripcion("Pueblo cafetero").ubicacion("Filandia, Quindío")
                .dificultad("BAJA").accesibilidad(true).precioBase(new BigDecimal("100000")).fotosUrl(List.of()).build());
        recorrido = actividad(filandia, "Recorrido por Filandia", 180);
        finca = actividad(filandia, "Visita a finca cafetera", 120);
        centro = actividad(filandia, "Visita al centro histórico", 120);
        nocturna = actividad(filandia, "Observación de estrellas", 120);
        em.flush();
        em.clear();
    }

    private Usuario usuario(String correo, String rol) {
        return em.persist(Usuario.builder().nombre("Nombre").apellidos("Apellido").correo(correo).contrasenaHash("hash").rol(rol).build());
    }

    private Actividad actividad(Destino destino, String nombre, int minutos) {
        return em.persist(Actividad.builder().destino(destino).nombre(nombre).descripcion("Descripción").duracionMinutos(minutos)
                .dificultad("BAJA").precioPorPersona(new BigDecimal("80000")).accesibilidad(true).build());
    }

    private ViajeActividadEntradaDto entrada(Actividad actividad, LocalDate fecha, String hora) {
        ViajeActividadEntradaDto dto = new ViajeActividadEntradaDto();
        dto.setActividadId(actividad.getId());
        dto.setFecha(fecha);
        dto.setHoraInicio(LocalTime.parse(hora));
        return dto;
    }

    private GuardarViajeDto viaje(String nombre, LocalDate inicio, LocalDate fin, ViajeActividadEntradaDto... actividades) {
        GuardarViajeDto dto = new GuardarViajeDto();
        dto.setNombre(nombre);
        dto.setFechaInicio(inicio);
        dto.setFechaFin(fin);
        dto.setActividades(new ArrayList<>(List.of(actividades)));
        return dto;
    }

    // ---------- crear ----------

    @Test
    void crearGuardaElViajeConLaPosicionDeCadaActividadDentroDeSuDia() {
        LocalDate dia1 = HOY.plusDays(10);
        LocalDate dia2 = dia1.plusDays(1);

        ViajeDto creado = service.crear(viaje("  Viaje al Quindío ", dia1, dia1.plusDays(3),
                entrada(recorrido, dia1, "09:00"), entrada(finca, dia1, "14:00"), entrada(centro, dia2, "10:00")), ana);

        assertNotNull(creado.getId());
        assertEquals("Viaje al Quindío", creado.getNombre());
        assertEquals(3, creado.getActividades().size());
        assertEquals(List.of(1, 2, 1), creado.getActividades().stream().map(ViajeActividadDto::getOrden).toList());
        assertEquals("Filandia, Quindío", creado.getActividades().get(0).getUbicacion());
        assertEquals(180, creado.getActividades().get(0).getDuracionMinutos());
    }

    @Test
    void laPosicionLaDefineLaHoraNoElOrdenEnQueLlegaLaLista() {
        LocalDate dia = HOY.plusDays(10);

        ViajeDto creado = service.crear(viaje("Desordenado", dia, dia,
                entrada(finca, dia, "14:00"), entrada(centro, dia, "17:00"), entrada(recorrido, dia, "09:00")), ana);

        assertEquals(List.of("Recorrido por Filandia", "Visita a finca cafetera", "Visita al centro histórico"),
                creado.getActividades().stream().map(ViajeActividadDto::getNombre).toList());
        assertEquals(List.of(1, 2, 3), creado.getActividades().stream().map(ViajeActividadDto::getOrden).toList());
    }

    @Test
    void crearSinActividadesEsValido() {
        ViajeDto creado = service.crear(viaje("Solo fechas", HOY.plusDays(5), HOY.plusDays(6)), ana);

        assertTrue(creado.getActividades().isEmpty());
    }

    @Test
    void elPropietarioSaleDelTokenYNoDelCuerpo() {
        ViajeDto creado = service.crear(viaje("De Ana", HOY.plusDays(5), HOY.plusDays(6)), ana);

        assertEquals(1, service.listar(ana).size());
        assertTrue(service.listar(luis).isEmpty());
        assertEquals(creado.getId(), service.listar(ana).get(0).getId());
    }

    // ---------- fechas ----------

    @Test
    void noSePuedeCrearConFechaDeInicioPasada() {
        assertThrows(ViajeInvalidoException.class,
                () -> service.crear(viaje("Pasado", HOY.minusDays(1), HOY.plusDays(2)), ana));
    }

    @Test
    void hoyEsUnaFechaDeInicioValida() {
        assertDoesNotThrow(() -> service.crear(viaje("Hoy", HOY, HOY.plusDays(1)), ana));
    }

    @Test
    void lafechaDeFinNoPuedeSerAnteriorALaDeInicio() {
        assertThrows(ViajeInvalidoException.class,
                () -> service.crear(viaje("Al revés", HOY.plusDays(5), HOY.plusDays(4)), ana));
    }

    @Test
    void unViajeYaIniciadoPuedeEditarseSinMoverSuFechaDeInicioPeroNoAlPasado() {
        Usuario propietario = em.find(Usuario.class, ana.id());
        Viaje enCurso = em.persist(Viaje.builder().usuario(propietario).nombre("En curso")
                .fechaInicio(HOY.minusDays(2)).fechaFin(HOY.plusDays(2)).build());
        em.flush();
        em.clear();

        ViajeDto editado = assertDoesNotThrow(() -> service.actualizar(enCurso.getId(),
                viaje("En curso (editado)", HOY.minusDays(2), HOY.plusDays(3)), ana));
        assertEquals("En curso (editado)", editado.getNombre());

        assertThrows(ViajeInvalidoException.class, () -> service.actualizar(enCurso.getId(),
                viaje("Moverlo", HOY.minusDays(5), HOY.plusDays(3)), ana));
    }

    // ---------- actividades dentro del viaje ----------

    @Test
    void unaActividadFueraDeLasFechasDelViajeSeRechaza() {
        LocalDate inicio = HOY.plusDays(5);

        assertThrows(ViajeInvalidoException.class, () -> service.crear(
                viaje("Fuera", inicio, inicio.plusDays(1), entrada(finca, inicio.plusDays(2), "09:00")), ana));
        assertThrows(ViajeInvalidoException.class, () -> service.crear(
                viaje("Antes", inicio, inicio.plusDays(1), entrada(finca, inicio.minusDays(1), "09:00")), ana));
    }

    @Test
    void unaActividadQueTerminariaDespuesDeLaMedianocheSeRechaza() {
        LocalDate dia = HOY.plusDays(5);

        assertThrows(ViajeInvalidoException.class, () -> service.crear(
                viaje("Noche", dia, dia, entrada(nocturna, dia, "22:30")), ana));
        assertDoesNotThrow(() -> service.crear(viaje("Justo", dia, dia, entrada(nocturna, dia, "22:00")), ana));
    }

    @Test
    void unaActividadInexistenteDevuelveNoEncontrado() {
        LocalDate dia = HOY.plusDays(5);
        ViajeActividadEntradaDto fantasma = entrada(finca, dia, "09:00");
        fantasma.setActividadId(99999L);

        assertThrows(RecursoNoEncontradoException.class, () -> service.crear(viaje("Fantasma", dia, dia, fantasma), ana));
    }

    // ---------- solapes ----------

    @Test
    void dosActividadesQueSeSolapanBloqueanElGuardado() {
        LocalDate dia = HOY.plusDays(5);

        HorarioSolapadoException error = assertThrows(HorarioSolapadoException.class, () -> service.crear(
                viaje("Solape", dia, dia, entrada(recorrido, dia, "09:00"), entrada(finca, dia, "11:00")), ana));

        assertTrue(error.getMessage().contains("Recorrido por Filandia"));
        assertTrue(error.getMessage().contains("Visita a finca cafetera"));
        assertTrue(service.listar(ana).isEmpty(), "No debe guardarse nada si hay solape");
    }

    @Test
    void elOrdenEnQueSeEnviaLaListaNoEvitaElSolape() {
        LocalDate dia = HOY.plusDays(5);

        assertThrows(HorarioSolapadoException.class, () -> service.crear(
                viaje("Desordenadas", dia, dia, entrada(finca, dia, "11:00"), entrada(recorrido, dia, "09:00")), ana));
    }

    @Test
    void terminarJustoCuandoEmpiezaLaSiguienteNoEsSolape() {
        LocalDate dia = HOY.plusDays(5);

        assertDoesNotThrow(() -> service.crear(
                viaje("Pegadas", dia, dia, entrada(recorrido, dia, "09:00"), entrada(finca, dia, "12:00")), ana));
    }

    @Test
    void unaActividadLargaQueCubreVariasPosterioresSeDetecta() {
        LocalDate dia = HOY.plusDays(5);

        // 09:00-12:00 cubre a las dos siguientes aunque entre ellas no se solapen (11:00-13:00 y 13:00-15:00)
        assertThrows(HorarioSolapadoException.class, () -> service.crear(
                viaje("Cubre", dia, dia, entrada(recorrido, dia, "09:00"), entrada(finca, dia, "11:00"), entrada(centro, dia, "13:00")), ana));
    }

    @Test
    void actividadesDeDiasDistintosNuncaSeSolapan() {
        LocalDate dia = HOY.plusDays(5);

        assertDoesNotThrow(() -> service.crear(viaje("Dos días", dia, dia.plusDays(1),
                entrada(recorrido, dia, "09:00"), entrada(recorrido, dia.plusDays(1), "09:00")), ana));
    }

    // ---------- consultar y actualizar ----------

    @Test
    void unViajeAjenoEsIndistinguibleDeUnoInexistenteTambienParaElAdministrador() {
        Long id = service.crear(viaje("De Ana", HOY.plusDays(5), HOY.plusDays(6)), ana).getId();

        assertEquals("De Ana", service.obtenerPorId(id, ana).getNombre());
        assertThrows(RecursoNoEncontradoException.class, () -> service.obtenerPorId(id, luis));
        assertThrows(RecursoNoEncontradoException.class, () -> service.obtenerPorId(id, admin));
        assertThrows(RecursoNoEncontradoException.class, () -> service.obtenerPorId(99999L, ana));
    }

    @Test
    void nadieEditaUnViajeAjeno() {
        Long id = service.crear(viaje("De Ana", HOY.plusDays(5), HOY.plusDays(6)), ana).getId();

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.actualizar(id, viaje("Robado", HOY.plusDays(5), HOY.plusDays(6)), luis));
        assertThrows(RecursoNoEncontradoException.class,
                () -> service.actualizar(id, viaje("Robado", HOY.plusDays(5), HOY.plusDays(6)), admin));
        assertEquals("De Ana", service.obtenerPorId(id, ana).getNombre());
    }

    @Test
    void actualizarReemplazaLasActividadesYReasignaLasPosiciones() {
        LocalDate dia = HOY.plusDays(5);
        Long id = service.crear(viaje("Original", dia, dia.plusDays(1),
                entrada(recorrido, dia, "09:00"), entrada(finca, dia, "14:00")), ana).getId();

        ViajeDto editado = service.actualizar(id, viaje("Editado", dia, dia.plusDays(1),
                entrada(finca, dia, "14:00"), entrada(centro, dia, "10:00"), entrada(recorrido, dia.plusDays(1), "09:00")), ana);

        assertEquals("Editado", editado.getNombre());
        // Cronológico dentro de cada día, aunque la lista se enviara con la finca (14:00) antes que el centro (10:00)
        assertEquals(List.of("Visita al centro histórico", "Visita a finca cafetera", "Recorrido por Filandia"),
                editado.getActividades().stream().map(ViajeActividadDto::getNombre).toList());
        assertEquals(List.of(1, 2, 1), editado.getActividades().stream().map(ViajeActividadDto::getOrden).toList());

        em.flush();
        em.clear();
        assertEquals(3, service.obtenerPorId(id, ana).getActividades().size());
    }

    @Test
    void actualizarConSolapeNoCambiaNada() {
        LocalDate dia = HOY.plusDays(5);
        Long id = service.crear(viaje("Original", dia, dia, entrada(recorrido, dia, "09:00")), ana).getId();

        assertThrows(HorarioSolapadoException.class, () -> service.actualizar(id,
                viaje("Editado", dia, dia, entrada(recorrido, dia, "09:00"), entrada(finca, dia, "10:00")), ana));

        em.clear();
        ViajeDto intacto = service.obtenerPorId(id, ana);
        assertEquals("Original", intacto.getNombre());
        assertEquals(1, intacto.getActividades().size());
    }

    @Test
    void elResumenMuestraUbicacionYTotalDeActividadesOrdenadoPorFecha() {
        service.crear(viaje("Lejano", HOY.plusDays(30), HOY.plusDays(31)), ana);
        LocalDate cerca = HOY.plusDays(3);
        service.crear(viaje("Cercano", cerca, cerca.plusDays(1), entrada(recorrido, cerca, "09:00"), entrada(finca, cerca, "14:00")), ana);
        em.flush();
        em.clear();

        List<ViajeResumenDto> resumen = service.listar(ana);

        assertEquals(List.of("Cercano", "Lejano"), resumen.stream().map(ViajeResumenDto::getNombre).toList());
        assertEquals(2, resumen.get(0).getTotalActividades());
        assertEquals("Filandia, Quindío", resumen.get(0).getUbicacion());
        assertEquals(0, resumen.get(1).getTotalActividades());
        assertNull(resumen.get(1).getUbicacion());
    }

    @Test
    void elResumenIncluyeLaHoraDeLaPrimeraActividadYNingunaSiNoHay() {
        LocalDate dia = HOY.plusDays(3);
        service.crear(viaje("Con actividades", dia, dia, entrada(finca, dia, "14:00"), entrada(recorrido, dia, "09:00")), ana);
        service.crear(viaje("Vacío", HOY.plusDays(30), HOY.plusDays(31)), ana);
        em.flush();
        em.clear();

        List<ViajeResumenDto> resumen = service.listar(ana);

        assertEquals(LocalTime.of(9, 0), resumen.get(0).getPrimeraHora(), "La primera es la más temprana, no la primera enviada");
        assertNull(resumen.get(1).getPrimeraHora());
    }

    // ---------- eliminar ----------

    @Test
    void eliminarBorraElViajeYSusActividadesPeroNoElCatalogo() {
        LocalDate dia = HOY.plusDays(5);
        Long id = service.crear(viaje("Para borrar", dia, dia, entrada(recorrido, dia, "09:00"), entrada(finca, dia, "14:00")), ana).getId();
        service.crear(viaje("Se queda", dia, dia, entrada(centro, dia, "10:00")), ana);
        em.flush();
        em.clear();

        service.eliminar(id, ana);
        em.flush();
        em.clear();

        assertThrows(RecursoNoEncontradoException.class, () -> service.obtenerPorId(id, ana));
        assertEquals(List.of("Se queda"), service.listar(ana).stream().map(ViajeResumenDto::getNombre).toList());
        Long actividadesColocadas = em.getEntityManager().createQuery("select count(va) from ViajeActividad va", Long.class).getSingleResult();
        assertEquals(1L, actividadesColocadas, "Solo debe quedar la actividad del otro viaje");
        assertNotNull(em.find(Actividad.class, recorrido.getId()), "El catálogo de actividades no se toca");
    }

    @Test
    void nadieEliminaUnViajeAjenoNiSiquieraElAdministrador() {
        Long id = service.crear(viaje("De Ana", HOY.plusDays(5), HOY.plusDays(6)), ana).getId();

        assertThrows(RecursoNoEncontradoException.class, () -> service.eliminar(id, luis));
        assertThrows(RecursoNoEncontradoException.class, () -> service.eliminar(id, admin));
        assertThrows(RecursoNoEncontradoException.class, () -> service.eliminar(99999L, ana));
        assertEquals("De Ana", service.obtenerPorId(id, ana).getNombre());
    }
}
