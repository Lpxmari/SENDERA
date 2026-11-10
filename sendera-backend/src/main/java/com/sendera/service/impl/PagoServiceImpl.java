package com.sendera.service.impl;

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
import com.sendera.service.PagoService;
import com.sendera.service.ReservaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class PagoServiceImpl implements PagoService {

    /** Referencia externa enviada a Mercado Pago: "SD-" + id de la reserva con 6 dígitos (p. ej. SD-000015). */
    private static final String PREFIJO_REFERENCIA = "SD-";
    private static final String MONEDA = "COP";
    private static final int LARGO_MAXIMO_TITULO = 250;
    private static final Pattern ID_PAGO = Pattern.compile("\\d{1,20}");

    private final ReservaRepository reservaRepository;
    private final PagoRepository pagoRepository;
    private final ReservaService reservaService;
    private final MercadoPagoClient mercadoPagoClient;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.pagos.mercadopago.notification-url:}")
    private String urlNotificacion;

    @Override
    @Transactional
    public PreferenciaPagoDto iniciarPago(Long reservaId, UsuarioActual solicitante) {
        if (!mercadoPagoClient.estaConfigurado()) {
            throw new PagoNoDisponibleException();
        }
        // Solo el titular paga su reserva; para cualquier otra persona la reserva "no existe" (404)
        Reserva reserva = reservaRepository.findByIdAndUsuarioId(reservaId, solicitante.id())
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada con id: " + reservaId));
        Pago pago = pagoRepository.findByReservaId(reservaId).orElse(null);
        validarQueSePuedePagar(reserva, pago);

        PreferenciaMercadoPago preferencia = mercadoPagoClient.crearPreferencia(solicitudPara(reserva));

        LocalDateTime ahora = LocalDateTime.now();
        if (pago == null) {
            pago = Pago.builder().reserva(reserva).fechaCreacion(ahora).build();
        }
        pago.setPreferenciaId(preferencia.id());
        // El monto sale de la reserva, nunca del cliente
        pago.setMonto(reserva.getMontoTotal());
        pago.setEstadoPago(EstadoPago.PENDIENTE);
        pago.setFechaActualizacion(ahora);
        pagoRepository.save(pago);
        return new PreferenciaPagoDto(preferencia.id(), mercadoPagoClient.urlCheckout(preferencia));
    }

    @Override
    @Transactional
    public ReservaDto verificarPago(Long reservaId, UsuarioActual solicitante) {
        // Comprueba la propiedad (404 para terceros) antes de consultar a Mercado Pago
        reservaService.obtenerPorId(reservaId, solicitante);
        if (mercadoPagoClient.estaConfigurado() && pagoRepository.findByReservaId(reservaId).isPresent()) {
            elegirPagoVigente(mercadoPagoClient.buscarPagosPorReferencia(referenciaDe(reservaId)))
                    .ifPresent(this::aplicar);
        }
        return reservaService.obtenerPorId(reservaId, solicitante);
    }

    @Override
    @Transactional
    public void procesarNotificacion(String tipo, String pagoId) {
        if (!"payment".equals(tipo)) {
            return;
        }
        if (pagoId == null || !ID_PAGO.matcher(pagoId).matches()) {
            log.warn("Se recibió una notificación de pago de Mercado Pago sin un identificador válido.");
            return;
        }
        if (!mercadoPagoClient.estaConfigurado()) {
            log.warn("Llegó una notificación de Mercado Pago, pero MERCADOPAGO_ACCESS_TOKEN no está configurado.");
            return;
        }
        mercadoPagoClient.obtenerPago(pagoId).ifPresentOrElse(this::aplicar,
                () -> log.info("Mercado Pago no encontró el pago {} de la notificación; se ignora.", pagoId));
    }

    private void validarQueSePuedePagar(Reserva reserva, Pago pago) {
        if (reserva.getEstadoReserva() == EstadoReserva.CANCELADA) {
            throw new ReservaNoPermitidaException("La reserva fue cancelada y ya no se puede pagar.");
        }
        if (pago != null && pago.getEstadoPago() == EstadoPago.APROBADO) {
            throw new ReservaNoPermitidaException("Esta reserva ya fue pagada.");
        }
        if (reserva.getEstadoReserva() == EstadoReserva.CONFIRMADA) {
            throw new ReservaNoPermitidaException("Esta reserva ya está confirmada; no necesitas pagarla en línea.");
        }
    }

    private SolicitudPreferencia solicitudPara(Reserva reserva) {
        Itinerario itinerario = reserva.getItinerario();
        String referencia = referenciaDe(reserva.getId());
        String titulo = "Reserva " + referencia + " - " + itinerario.getNombre();
        String urlRetorno = sinBarraFinal(frontendUrl) + "/pago/resultado?reserva=" + reserva.getId();
        return new SolicitudPreferencia(
                "itinerario-" + itinerario.getId(),
                titulo.length() > LARGO_MAXIMO_TITULO ? titulo.substring(0, LARGO_MAXIMO_TITULO) : titulo,
                reserva.getMontoTotal(),
                MONEDA,
                referencia,
                urlRetorno,
                // Mercado Pago rechaza auto_return con URLs locales; en local se vuelve con "Volver al sitio"
                !esUrlLocal(frontendUrl),
                urlNotificacion);
    }

    /** Aplica a la reserva el estado de un pago consultado en Mercado Pago. Es idempotente. */
    private void aplicar(PagoMercadoPago pagoMp) {
        Long reservaId = reservaDeReferencia(pagoMp.externalReference());
        if (reservaId == null) {
            log.info("El pago {} de Mercado Pago no pertenece a una reserva de SENDERA; se ignora.", pagoMp.id());
            return;
        }
        Pago pago = pagoRepository.buscarParaActualizar(reservaId).orElse(null);
        if (pago == null) {
            log.warn("El pago {} es de la reserva {}, que no tiene un pago iniciado; se ignora.", pagoMp.id(), reservaId);
            return;
        }

        EstadoPago nuevo = estadoDe(pagoMp.status());
        String pagoExternoId = String.valueOf(pagoMp.id());
        // Un pago aprobado solo cambia si ese mismo pago se reembolsa: los intentos rechazados anteriores pueden llegar tarde
        if (pago.getEstadoPago() == EstadoPago.APROBADO
                && !(nuevo == EstadoPago.REEMBOLSADO && pagoExternoId.equals(pago.getPagoExternoId()))) {
            return;
        }
        if (nuevo == EstadoPago.APROBADO && !coincideMonto(pago, pagoMp)) {
            log.warn("El pago {} de la reserva {} no coincide con el monto o la moneda esperados; no se confirma.",
                    pagoMp.id(), reservaId);
            return;
        }

        pago.setEstadoPago(nuevo);
        pago.setPagoExternoId(pagoExternoId);
        pago.setFechaActualizacion(LocalDateTime.now());

        Reserva reserva = pago.getReserva();
        if (nuevo == EstadoPago.APROBADO) {
            if (reserva.getEstadoReserva() == EstadoReserva.PENDIENTE) {
                reserva.setEstadoReserva(EstadoReserva.CONFIRMADA);
            } else if (reserva.getEstadoReserva() == EstadoReserva.CANCELADA) {
                log.warn("Se aprobó el pago {} de la reserva {}, que ya estaba cancelada: requiere reembolso manual.",
                        pagoMp.id(), reservaId);
            }
        }
    }

    /** Si hay un pago aprobado manda ese; si no, el intento más reciente (la búsqueda llega ordenada así). */
    private static Optional<PagoMercadoPago> elegirPagoVigente(List<PagoMercadoPago> pagos) {
        return pagos.stream().filter(p -> "approved".equals(p.status())).findFirst()
                .or(() -> pagos.stream().findFirst());
    }

    private static EstadoPago estadoDe(String estadoMercadoPago) {
        return switch (estadoMercadoPago == null ? "" : estadoMercadoPago) {
            case "approved" -> EstadoPago.APROBADO;
            case "rejected", "cancelled" -> EstadoPago.RECHAZADO;
            case "refunded", "charged_back" -> EstadoPago.REEMBOLSADO;
            // pending, in_process, in_mediation, authorized
            default -> EstadoPago.PENDIENTE;
        };
    }

    private static boolean coincideMonto(Pago pago, PagoMercadoPago pagoMp) {
        return pagoMp.transactionAmount() != null
                && pagoMp.transactionAmount().compareTo(pago.getMonto()) == 0
                && MONEDA.equals(pagoMp.currencyId());
    }

    static String referenciaDe(Long reservaId) {
        return String.format("%s%06d", PREFIJO_REFERENCIA, reservaId);
    }

    static Long reservaDeReferencia(String referencia) {
        if (referencia == null || !referencia.startsWith(PREFIJO_REFERENCIA)) {
            return null;
        }
        try {
            return Long.parseLong(referencia.substring(PREFIJO_REFERENCIA.length()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static boolean esUrlLocal(String url) {
        try {
            String host = URI.create(url).getHost();
            if (host == null) {
                return true;
            }
            host = host.toLowerCase(Locale.ROOT);
            return host.equals("localhost") || host.endsWith(".localhost")
                    || host.equals("127.0.0.1") || host.equals("[::1]");
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private static String sinBarraFinal(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
