package com.sendera.service.impl;

import com.sendera.dto.CambiarEstadoReservaDto;
import com.sendera.dto.PaginaDto;
import com.sendera.dto.ReservaAdminDto;
import com.sendera.exception.FiltroInvalidoException;
import com.sendera.exception.RecursoNoEncontradoException;
import com.sendera.exception.ReservaNoPermitidaException;
import com.sendera.model.EstadoReserva;
import com.sendera.model.Itinerario;
import com.sendera.model.Reserva;
import com.sendera.repository.ItinerarioRepository;
import com.sendera.repository.ReservaRepository;
import com.sendera.security.UsuarioActual;
import com.sendera.service.AuditoriaService;
import com.sendera.service.ReservaAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ReservaAdminServiceImpl implements ReservaAdminService {

    private static final int TAMANO_MAXIMO = 100;

    private final ReservaRepository reservaRepository;
    private final ItinerarioRepository itinerarioRepository;
    private final AuditoriaService auditoriaService;

    @Override
    @Transactional(readOnly = true)
    public PaginaDto<ReservaAdminDto> listar(String estado, String q, LocalDate desde, LocalDate hasta,
                                             int pagina, int tamano) {
        if (pagina < 0 || tamano < 1 || tamano > TAMANO_MAXIMO) {
            throw new FiltroInvalidoException("La página debe ser 0 o mayor y el tamaño estar entre 1 y " + TAMANO_MAXIMO + ".");
        }
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new FiltroInvalidoException("La fecha inicial no puede ser posterior a la final.");
        }
        Page<Reserva> resultado = reservaRepository.buscarAdmin(parsearEstado(estado), desde, hasta, patron(q),
                PageRequest.of(pagina, tamano, Sort.by(Sort.Direction.DESC, "fechaReserva").and(Sort.by(Sort.Direction.DESC, "id"))));
        return PaginaDto.<ReservaAdminDto>builder()
                .contenido(resultado.getContent().stream().map(this::convertir).toList())
                .pagina(pagina)
                .tamano(tamano)
                .total(resultado.getTotalElements())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ReservaAdminDto obtener(Long id) {
        return convertir(buscar(id));
    }

    /**
     * Transiciones permitidas: PENDIENTE → CONFIRMADA, PENDIENTE → CANCELADA y CONFIRMADA → CANCELADA.
     * CANCELADA es final. Al cancelar se devuelve el cupo al itinerario (con bloqueo, como al reservar).
     */
    @Override
    @Transactional
    public ReservaAdminDto cambiarEstado(Long id, CambiarEstadoReservaDto dto, UsuarioActual actor) {
        Reserva reserva = buscar(id);
        EstadoReserva anterior = reserva.getEstadoReserva();
        EstadoReserva nuevo = EstadoReserva.valueOf(dto.getEstado());

        if (anterior == EstadoReserva.CANCELADA) {
            throw new ReservaNoPermitidaException("La reserva ya fue cancelada y no puede cambiar de estado.");
        }
        if (anterior == nuevo) {
            throw new ReservaNoPermitidaException("La reserva ya está " + nuevo.name().toLowerCase(Locale.ROOT) + ".");
        }

        if (nuevo == EstadoReserva.CANCELADA) {
            Itinerario itinerario = itinerarioRepository.buscarParaReservar(reserva.getItinerario().getId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Itinerario de la reserva no encontrado."));
            itinerario.setCuposDisponibles(itinerario.getCuposDisponibles() + 1);
        }
        reserva.setEstadoReserva(nuevo);
        reservaRepository.save(reserva);
        auditoriaService.registrar(actor, nuevo == EstadoReserva.CONFIRMADA ? "RESERVA_CONFIRMAR" : "RESERVA_CANCELAR",
                "reserva", id, anterior + " -> " + nuevo);
        return convertir(reserva);
    }

    private EstadoReserva parsearEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return null;
        }
        try {
            return EstadoReserva.valueOf(estado.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new FiltroInvalidoException("El estado debe ser PENDIENTE, CONFIRMADA o CANCELADA.");
        }
    }

    /** Minúsculas y comodines escapados (con '!') para que el texto del administrador no actúe como patrón LIKE. */
    private String patron(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        return "%" + q.trim().toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }

    private Reserva buscar(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada con id: " + id));
    }

    private ReservaAdminDto convertir(Reserva r) {
        return ReservaAdminDto.builder()
                .id(r.getId())
                .usuarioId(r.getUsuario().getId())
                .usuarioNombre(r.getUsuario().getNombre() + " " + r.getUsuario().getApellidos())
                .usuarioCorreo(r.getUsuario().getCorreo())
                .itinerarioId(r.getItinerario().getId())
                .itinerarioNombre(r.getItinerario().getNombre())
                .destinoNombre(r.getItinerario().getDestino().getNombre())
                .fechaReserva(r.getFechaReserva())
                .montoTotal(r.getMontoTotal())
                .estadoReserva(r.getEstadoReserva().name())
                .build();
    }
}
