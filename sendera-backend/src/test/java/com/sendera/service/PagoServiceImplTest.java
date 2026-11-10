package com.sendera.service;

import com.sendera.dto.PreferenciaPagoDto;
import com.sendera.dto.ReservaDto;
import com.sendera.exception.PagoNoDisponibleException;
import com.sendera.exception.RecursoNoEncontradoException;
import com.sendera.exception.ReservaNoPermitidaException;
import com.sendera.integration.MercadoPagoClient;
import com.sendera.integration.PagoMercadoPago;
import com.sendera.integration.PreferenciaMercadoPago;
import com.sendera.integration.SolicitudPreferencia;
import com.sendera.model.EstadoPago;
import com.sendera.model.EstadoReserva;
import com.sendera.model.Itinerario;
import com.sendera.model.Pago;
import com.sendera.model.Reserva;
import com.sendera.repository.PagoRepository;
import com.sendera.repository.ReservaRepository;
import com.sendera.security.UsuarioActual;
import com.sendera.service.impl.PagoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** RF-014: pago de reservas con Mercado Pago Checkout Pro. */
@ExtendWith(MockitoExtension.class)
class PagoServiceImplTest {

    private static final UsuarioActual CLIENTE = new UsuarioActual(5L, false);

    @Mock ReservaRepository reservaRepository;
    @Mock PagoRepository pagoRepository;
    @Mock ReservaService reservaService;
    @Mock MercadoPagoClient mercadoPagoClient;
    @InjectMocks PagoServiceImpl pagoService;

    private Reserva reserva;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(pagoService, "frontendUrl", "https://sendera.app/");
        ReflectionTestUtils.setField(pagoService, "urlNotificacion", "");
        Itinerario itinerario = Itinerario.builder().id(2L).nombre("Descanso cafetero").build();
        reserva = Reserva.builder().id(15L).itinerario(itinerario).fechaReserva(LocalDate.of(2026, 11, 18))
                .montoTotal(new BigDecimal("195000.00")).estadoReserva(EstadoReserva.PENDIENTE).build();
    }

    private Pago pagoIniciado(EstadoPago estado, String pagoExternoId) {
        return Pago.builder().id(1L).reserva(reserva).preferenciaId("pref-1").monto(new BigDecimal("195000.00"))
                .estadoPago(estado).pagoExternoId(pagoExternoId).build();
    }

    private static PagoMercadoPago pagoMp(long id, String estado, String monto) {
        return new PagoMercadoPago(id, estado, "SD-000015", new BigDecimal(monto), "COP");
    }

    // ---- Iniciar pago ----

    @Test
    void iniciarPagoCreaLaPreferenciaConDatosDelServidorYGuardaElPago() {
        PreferenciaMercadoPago preferencia = new PreferenciaMercadoPago("pref-9", "https://mp/checkout", null);
        when(mercadoPagoClient.estaConfigurado()).thenReturn(true);
        when(reservaRepository.findByIdAndUsuarioId(15L, 5L)).thenReturn(Optional.of(reserva));
        when(pagoRepository.findByReservaId(15L)).thenReturn(Optional.empty());
        when(mercadoPagoClient.crearPreferencia(any())).thenReturn(preferencia);
        when(mercadoPagoClient.urlCheckout(preferencia)).thenReturn("https://mp/checkout");

        PreferenciaPagoDto resultado = pagoService.iniciarPago(15L, CLIENTE);

        assertEquals("pref-9", resultado.getPreferenciaId());
        assertEquals("https://mp/checkout", resultado.getUrlPago());

        ArgumentCaptor<SolicitudPreferencia> solicitud = ArgumentCaptor.forClass(SolicitudPreferencia.class);
        verify(mercadoPagoClient).crearPreferencia(solicitud.capture());
        assertEquals(0, new BigDecimal("195000.00").compareTo(solicitud.getValue().monto()));
        assertEquals("COP", solicitud.getValue().moneda());
        assertEquals("SD-000015", solicitud.getValue().referenciaExterna());
        assertEquals("https://sendera.app/pago/resultado?reserva=15", solicitud.getValue().urlRetorno());
        assertTrue(solicitud.getValue().autoRetorno());
        assertTrue(solicitud.getValue().titulo().contains("Descanso cafetero"));

        ArgumentCaptor<Pago> guardado = ArgumentCaptor.forClass(Pago.class);
        verify(pagoRepository).save(guardado.capture());
        assertEquals("pref-9", guardado.getValue().getPreferenciaId());
        assertEquals(EstadoPago.PENDIENTE, guardado.getValue().getEstadoPago());
        assertSame(reserva, guardado.getValue().getReserva());
    }

    @Test
    void enLocalNoSePideRetornoAutomatico() {
        ReflectionTestUtils.setField(pagoService, "frontendUrl", "http://localhost:5173");
        when(mercadoPagoClient.estaConfigurado()).thenReturn(true);
        when(reservaRepository.findByIdAndUsuarioId(15L, 5L)).thenReturn(Optional.of(reserva));
        when(mercadoPagoClient.crearPreferencia(any())).thenReturn(new PreferenciaMercadoPago("p", "https://mp", null));

        pagoService.iniciarPago(15L, CLIENTE);

        ArgumentCaptor<SolicitudPreferencia> solicitud = ArgumentCaptor.forClass(SolicitudPreferencia.class);
        verify(mercadoPagoClient).crearPreferencia(solicitud.capture());
        assertFalse(solicitud.getValue().autoRetorno());
        assertEquals("http://localhost:5173/pago/resultado?reserva=15", solicitud.getValue().urlRetorno());
    }

    @Test
    void reintentarTrasUnRechazoReutilizaElPago() {
        Pago rechazado = pagoIniciado(EstadoPago.RECHAZADO, "77");
        when(mercadoPagoClient.estaConfigurado()).thenReturn(true);
        when(reservaRepository.findByIdAndUsuarioId(15L, 5L)).thenReturn(Optional.of(reserva));
        when(pagoRepository.findByReservaId(15L)).thenReturn(Optional.of(rechazado));
        when(mercadoPagoClient.crearPreferencia(any())).thenReturn(new PreferenciaMercadoPago("pref-2", "https://mp", null));

        pagoService.iniciarPago(15L, CLIENTE);

        verify(pagoRepository).save(rechazado);
        assertEquals("pref-2", rechazado.getPreferenciaId());
        assertEquals(EstadoPago.PENDIENTE, rechazado.getEstadoPago());
    }

    @Test
    void sinCredencialesElPagoNoEstaDisponible() {
        when(mercadoPagoClient.estaConfigurado()).thenReturn(false);

        assertThrows(PagoNoDisponibleException.class, () -> pagoService.iniciarPago(15L, CLIENTE));
        verify(reservaRepository, never()).findByIdAndUsuarioId(any(), any());
    }

    @Test
    void nadiePagaLaReservaDeOtraPersona() {
        when(mercadoPagoClient.estaConfigurado()).thenReturn(true);
        when(reservaRepository.findByIdAndUsuarioId(15L, 5L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> pagoService.iniciarPago(15L, CLIENTE));
        verify(mercadoPagoClient, never()).crearPreferencia(any());
    }

    @Test
    void noSePagaUnaReservaCanceladaConfirmadaOYaPagada() {
        when(mercadoPagoClient.estaConfigurado()).thenReturn(true);
        when(reservaRepository.findByIdAndUsuarioId(15L, 5L)).thenReturn(Optional.of(reserva));

        reserva.setEstadoReserva(EstadoReserva.CANCELADA);
        assertThrows(ReservaNoPermitidaException.class, () -> pagoService.iniciarPago(15L, CLIENTE));

        reserva.setEstadoReserva(EstadoReserva.CONFIRMADA);
        assertThrows(ReservaNoPermitidaException.class, () -> pagoService.iniciarPago(15L, CLIENTE));

        reserva.setEstadoReserva(EstadoReserva.PENDIENTE);
        when(pagoRepository.findByReservaId(15L)).thenReturn(Optional.of(pagoIniciado(EstadoPago.APROBADO, "1")));
        assertThrows(ReservaNoPermitidaException.class, () -> pagoService.iniciarPago(15L, CLIENTE));

        verify(mercadoPagoClient, never()).crearPreferencia(any());
    }

    // ---- Notificaciones (webhook) ----

    @Test
    void unPagoAprobadoConfirmaLaReserva() {
        Pago pago = pagoIniciado(EstadoPago.PENDIENTE, null);
        when(mercadoPagoClient.estaConfigurado()).thenReturn(true);
        when(mercadoPagoClient.obtenerPago("987")).thenReturn(Optional.of(pagoMp(987, "approved", "195000")));
        when(pagoRepository.buscarParaActualizar(15L)).thenReturn(Optional.of(pago));

        pagoService.procesarNotificacion("payment", "987");

        assertEquals(EstadoPago.APROBADO, pago.getEstadoPago());
        assertEquals("987", pago.getPagoExternoId());
        assertEquals(EstadoReserva.CONFIRMADA, reserva.getEstadoReserva());
    }

    @Test
    void unPagoRechazadoDejaLaReservaPendienteParaReintentar() {
        Pago pago = pagoIniciado(EstadoPago.PENDIENTE, null);
        when(mercadoPagoClient.estaConfigurado()).thenReturn(true);
        when(mercadoPagoClient.obtenerPago("5")).thenReturn(Optional.of(pagoMp(5, "rejected", "195000")));
        when(pagoRepository.buscarParaActualizar(15L)).thenReturn(Optional.of(pago));

        pagoService.procesarNotificacion("payment", "5");

        assertEquals(EstadoPago.RECHAZADO, pago.getEstadoPago());
        assertEquals(EstadoReserva.PENDIENTE, reserva.getEstadoReserva());
    }

    @Test
    void unMontoDistintoNoConfirmaLaReserva() {
        Pago pago = pagoIniciado(EstadoPago.PENDIENTE, null);
        when(mercadoPagoClient.estaConfigurado()).thenReturn(true);
        when(mercadoPagoClient.obtenerPago("987")).thenReturn(Optional.of(pagoMp(987, "approved", "1000")));
        when(pagoRepository.buscarParaActualizar(15L)).thenReturn(Optional.of(pago));

        pagoService.procesarNotificacion("payment", "987");

        assertEquals(EstadoPago.PENDIENTE, pago.getEstadoPago());
        assertEquals(EstadoReserva.PENDIENTE, reserva.getEstadoReserva());
    }

    @Test
    void unIntentoRechazadoQueLlegaTardeNoDeshaceUnPagoAprobado() {
        Pago pago = pagoIniciado(EstadoPago.APROBADO, "987");
        reserva.setEstadoReserva(EstadoReserva.CONFIRMADA);
        when(mercadoPagoClient.estaConfigurado()).thenReturn(true);
        when(mercadoPagoClient.obtenerPago("5")).thenReturn(Optional.of(pagoMp(5, "rejected", "195000")));
        when(pagoRepository.buscarParaActualizar(15L)).thenReturn(Optional.of(pago));

        pagoService.procesarNotificacion("payment", "5");

        assertEquals(EstadoPago.APROBADO, pago.getEstadoPago());
        assertEquals("987", pago.getPagoExternoId());
        assertEquals(EstadoReserva.CONFIRMADA, reserva.getEstadoReserva());
    }

    @Test
    void unReembolsoDelPagoAprobadoSeRegistra() {
        Pago pago = pagoIniciado(EstadoPago.APROBADO, "987");
        when(mercadoPagoClient.estaConfigurado()).thenReturn(true);
        when(mercadoPagoClient.obtenerPago("987")).thenReturn(Optional.of(pagoMp(987, "refunded", "195000")));
        when(pagoRepository.buscarParaActualizar(15L)).thenReturn(Optional.of(pago));

        pagoService.procesarNotificacion("payment", "987");

        assertEquals(EstadoPago.REEMBOLSADO, pago.getEstadoPago());
    }

    @Test
    void unPagoAprobadoDeUnaReservaCanceladaNoLaReactiva() {
        Pago pago = pagoIniciado(EstadoPago.PENDIENTE, null);
        reserva.setEstadoReserva(EstadoReserva.CANCELADA);
        when(mercadoPagoClient.estaConfigurado()).thenReturn(true);
        when(mercadoPagoClient.obtenerPago("987")).thenReturn(Optional.of(pagoMp(987, "approved", "195000")));
        when(pagoRepository.buscarParaActualizar(15L)).thenReturn(Optional.of(pago));

        pagoService.procesarNotificacion("payment", "987");

        assertEquals(EstadoPago.APROBADO, pago.getEstadoPago());
        assertEquals(EstadoReserva.CANCELADA, reserva.getEstadoReserva());
    }

    @Test
    void ignoraNotificacionesQueNoSonDePagosOSinIdValido() {
        pagoService.procesarNotificacion("merchant_order", "1");
        pagoService.procesarNotificacion(null, "1");
        pagoService.procesarNotificacion("payment", null);
        pagoService.procesarNotificacion("payment", "1;DROP");

        verify(mercadoPagoClient, never()).obtenerPago(anyString());
    }

    @Test
    void ignoraPagosAjenosOInexistentes() {
        when(mercadoPagoClient.estaConfigurado()).thenReturn(true);
        when(mercadoPagoClient.obtenerPago("1")).thenReturn(Optional.of(
                new PagoMercadoPago(1L, "approved", "OTRA-TIENDA-1", BigDecimal.TEN, "COP")));
        when(mercadoPagoClient.obtenerPago("2")).thenReturn(Optional.empty());

        pagoService.procesarNotificacion("payment", "1");
        pagoService.procesarNotificacion("payment", "2");

        verify(pagoRepository, never()).buscarParaActualizar(any());
    }

    @Test
    void sinCredencialesLasNotificacionesSeIgnoran() {
        when(mercadoPagoClient.estaConfigurado()).thenReturn(false);

        pagoService.procesarNotificacion("payment", "987");

        verify(mercadoPagoClient, never()).obtenerPago(anyString());
    }

    // ---- Verificación desde el frontend ----

    @Test
    void verificarAplicaElPagoAprobadoAunqueHayaIntentosPosteriores() {
        Pago pago = pagoIniciado(EstadoPago.PENDIENTE, null);
        ReservaDto dto = ReservaDto.builder().id(15L).estadoReserva("CONFIRMADA").estadoPago("APROBADO").build();
        when(reservaService.obtenerPorId(15L, CLIENTE)).thenReturn(dto);
        when(mercadoPagoClient.estaConfigurado()).thenReturn(true);
        when(pagoRepository.findByReservaId(15L)).thenReturn(Optional.of(pago));
        when(mercadoPagoClient.buscarPagosPorReferencia("SD-000015")).thenReturn(List.of(
                pagoMp(3, "rejected", "195000"), pagoMp(2, "approved", "195000")));
        when(pagoRepository.buscarParaActualizar(15L)).thenReturn(Optional.of(pago));

        ReservaDto resultado = pagoService.verificarPago(15L, CLIENTE);

        assertSame(dto, resultado);
        assertEquals(EstadoPago.APROBADO, pago.getEstadoPago());
        assertEquals("2", pago.getPagoExternoId());
        assertEquals(EstadoReserva.CONFIRMADA, reserva.getEstadoReserva());
    }

    @Test
    void verificarSinPagoIniciadoNoConsultaAMercadoPago() {
        when(reservaService.obtenerPorId(15L, CLIENTE)).thenReturn(ReservaDto.builder().id(15L).build());
        when(mercadoPagoClient.estaConfigurado()).thenReturn(true);
        when(pagoRepository.findByReservaId(15L)).thenReturn(Optional.empty());

        assertNull(pagoService.verificarPago(15L, CLIENTE).getEstadoPago());
        verify(mercadoPagoClient, never()).buscarPagosPorReferencia(anyString());
    }

    @Test
    void verificarLaReservaDeOtraPersonaDevuelve404() {
        when(reservaService.obtenerPorId(15L, CLIENTE)).thenThrow(new RecursoNoEncontradoException("no"));

        assertThrows(RecursoNoEncontradoException.class, () -> pagoService.verificarPago(15L, CLIENTE));
        verify(mercadoPagoClient, never()).buscarPagosPorReferencia(anyString());
    }
}
