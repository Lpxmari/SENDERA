package com.sendera.service.impl;

import com.sendera.dto.CrearReservaDto;
import com.sendera.dto.ReservaDto;
import com.sendera.exception.RecursoNoEncontradoException;
import com.sendera.exception.ReservaNoPermitidaException;
import com.sendera.model.EstadoReserva;
import com.sendera.model.Itinerario;
import com.sendera.model.Reserva;
import com.sendera.repository.ItinerarioRepository;
import com.sendera.repository.ReservaRepository;
import com.sendera.repository.UsuarioRepository;
import com.sendera.security.UsuarioActual;
import com.sendera.service.ReservaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservaServiceImpl implements ReservaService {

    private final ReservaRepository reservaRepository;
    private final ItinerarioRepository itinerarioRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ReservaDto> listar(UsuarioActual solicitante) {
        List<Reserva> reservas = solicitante.admin()
                ? reservaRepository.findAll()
                : reservaRepository.findByUsuarioIdOrderByIdAsc(solicitante.id());
        return reservas.stream().map(this::convertirADto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ReservaDto obtenerPorId(Long id, UsuarioActual solicitante) {
        return convertirADto(buscarAutorizada(id, solicitante));
    }

    @Override
    @Transactional
    public ReservaDto crear(CrearReservaDto dto, UsuarioActual solicitante) {
        Itinerario itinerario = itinerarioRepository.buscarParaReservar(dto.getItinerarioId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Itinerario no encontrado con id: " + dto.getItinerarioId()));

        if (itinerario.getCuposDisponibles() == null || itinerario.getCuposDisponibles() < 1) {
            throw new ReservaNoPermitidaException("El itinerario seleccionado no tiene cupos disponibles.");
        }
        itinerario.setCuposDisponibles(itinerario.getCuposDisponibles() - 1);

        Reserva reserva = Reserva.builder()
                // El propietario sale del JWT, nunca del cuerpo de la petición
                .usuario(usuarioRepository.getReferenceById(solicitante.id()))
                .itinerario(itinerario)
                .fechaReserva(dto.getFechaReserva() != null ? dto.getFechaReserva() : LocalDate.now())
                // El monto sale del itinerario, nunca del cliente
                .montoTotal(itinerario.getPrecioBase())
                .estadoReserva(EstadoReserva.PENDIENTE)
                .build();

        return convertirADto(reservaRepository.save(reserva));
    }

    @Override
    @Transactional
    public ReservaDto cancelar(Long id, UsuarioActual solicitante) {
        Reserva reserva = buscarAutorizada(id, solicitante);
        if (reserva.getEstadoReserva() == EstadoReserva.CANCELADA) {
            throw new ReservaNoPermitidaException("La reserva ya fue cancelada.");
        }

        reserva.setEstadoReserva(EstadoReserva.CANCELADA);
        Itinerario itinerario = reserva.getItinerario();
        itinerario.setCuposDisponibles(itinerario.getCuposDisponibles() + 1);
        return convertirADto(reservaRepository.save(reserva));
    }

    /** Propietario o administrador; para cualquier otra persona la reserva "no existe" (404). */
    private Reserva buscarAutorizada(Long id, UsuarioActual solicitante) {
        return (solicitante.admin()
                ? reservaRepository.findById(id)
                : reservaRepository.findByIdAndUsuarioId(id, solicitante.id()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada con id: " + id));
    }

    private ReservaDto convertirADto(Reserva reserva) {
        return ReservaDto.builder()
                .id(reserva.getId())
                .itinerarioId(reserva.getItinerario() != null ? reserva.getItinerario().getId() : null)
                .fechaReserva(reserva.getFechaReserva())
                .montoTotal(reserva.getMontoTotal())
                .estadoReserva(reserva.getEstadoReserva() != null ? reserva.getEstadoReserva().name() : null)
                .build();
    }
}
