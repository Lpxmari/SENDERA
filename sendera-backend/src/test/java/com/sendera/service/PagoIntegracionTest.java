package com.sendera.service;

import com.sendera.dto.CrearReservaDto;
import com.sendera.dto.ReservaDto;
import com.sendera.integration.MercadoPagoClient;
import com.sendera.integration.PagoMercadoPago;
import com.sendera.integration.PreferenciaMercadoPago;
import com.sendera.model.Destino;
import com.sendera.model.Itinerario;
import com.sendera.model.Usuario;
import com.sendera.security.UsuarioActual;
import com.sendera.service.impl.PagoServiceImpl;
import com.sendera.service.impl.ReservaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** RF-014: flujo de pago contra una base H2 real; solo la API de Mercado Pago está simulada. */
@DataJpaTest
@ActiveProfiles("test")
@Import({ReservaServiceImpl.class, PagoServiceImpl.class})
@TestPropertySource(properties = "app.frontend-url=https://sendera.app")
class PagoIntegracionTest {

    @Autowired TestEntityManager em;
    @Autowired ReservaService reservaService;
    @Autowired PagoService pagoService;
    @MockBean MercadoPagoClient mercadoPagoClient;

    private UsuarioActual ana;
    private ReservaDto reserva;

    @BeforeEach
    void setUp() {
        Usuario usuario = em.persist(Usuario.builder().nombre("Ana").apellidos("Ruiz").correo("ana@correo.com")
                .contrasenaHash("hash").rol("ROLE_CLIENTE").build());
        ana = new UsuarioActual(usuario.getId(), false);
        Destino destino = em.persist(Destino.builder().nombre("Valle").descripcion("Paisaje").ubicacion("Salento")
                .dificultad("BAJA").accesibilidad(true).precioBase(new BigDecimal("85000.00"))
                .fotosUrl(new ArrayList<>()).build());
        Itinerario itinerario = em.persist(Itinerario.builder().destino(destino).nombre("Cocora").descripcion("Ruta")
                .fechaInicio(LocalDate.of(2026, 11, 18)).fechaFin(LocalDate.of(2026, 11, 19))
                .precioBase(new BigDecimal("195000.00")).cuposDisponibles(5).build());
        em.flush();

        CrearReservaDto dto = new CrearReservaDto();
        dto.setItinerarioId(itinerario.getId());
        dto.setFechaReserva(LocalDate.of(2026, 11, 18));
        reserva = reservaService.crear(dto, ana);

        when(mercadoPagoClient.estaConfigurado()).thenReturn(true);
        when(mercadoPagoClient.crearPreferencia(any())).thenReturn(new PreferenciaMercadoPago("pref-1", "https://mp/1", null));
        when(mercadoPagoClient.urlCheckout(any())).thenReturn("https://mp/1");
    }

    private String referencia() {
        return String.format("SD-%06d", reserva.getId());
    }

    @Test
    void elWebhookDeUnPagoAprobadoConfirmaLaReservaYSeVeEnElListado() {
        assertNull(reserva.getEstadoPago());

        pagoService.iniciarPago(reserva.getId(), ana);
        assertEquals("PENDIENTE", reservaService.listar(ana).get(0).getEstadoPago());

        when(mercadoPagoClient.obtenerPago("987")).thenReturn(Optional.of(
                new PagoMercadoPago(987L, "approved", referencia(), new BigDecimal("195000"), "COP")));
        pagoService.procesarNotificacion("payment", "987");
        em.flush();
        em.clear();

        ReservaDto actualizada = reservaService.listar(ana).get(0);
        assertEquals("CONFIRMADA", actualizada.getEstadoReserva());
        assertEquals("APROBADO", actualizada.getEstadoPago());
    }

    @Test
    void verificarTrasUnRechazoDejaLaReservaPendiente() {
        pagoService.iniciarPago(reserva.getId(), ana);
        when(mercadoPagoClient.buscarPagosPorReferencia(referencia())).thenReturn(List.of(
                new PagoMercadoPago(5L, "rejected", referencia(), new BigDecimal("195000"), "COP")));

        ReservaDto verificada = pagoService.verificarPago(reserva.getId(), ana);

        assertEquals("PENDIENTE", verificada.getEstadoReserva());
        assertEquals("RECHAZADO", verificada.getEstadoPago());
    }
}
