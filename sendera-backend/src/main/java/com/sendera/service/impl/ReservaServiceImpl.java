package com.sendera.service.impl;

import com.sendera.dto.CrearReservaDto;
import com.sendera.dto.ReservaDto;
import com.sendera.model.EstadoReserva;
import com.sendera.model.Itinerario;
import com.sendera.model.Reserva;
import com.sendera.repository.ReservaRepository;
import com.sendera.service.ReservaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservaServiceImpl implements ReservaService {

    private final ReservaRepository reservaRepository;

    @Override
    public List<ReservaDto> listarTodas() {
        return reservaRepository.findAll().stream()
                .map(this::convertirADto)
                .collect(Collectors.toList());
    }

    @Override
    public ReservaDto obtenerPorId(Long id) {
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada con id: " + id));
        return convertirADto(reserva);
    }

    @Override
    public ReservaDto crear(CrearReservaDto dto) {
        // Creamos una referencia al Itinerario para satisfacer la llave foránea
        Itinerario itinerario = new Itinerario();
        itinerario.setId(dto.getItinerarioId());

        Reserva reserva = Reserva.builder()
                .itinerario(itinerario)
                .fechaReserva(dto.getFechaReserva())
                .montoTotal(dto.getMontoTotal() != null ? dto.getMontoTotal() : BigDecimal.ZERO)
                .estadoPago(EstadoReserva.PENDIENTE)
                .build();

        Reserva reservaGuardada = reservaRepository.save(reserva);
        return convertirADto(reservaGuardada);
    }

    @Override
    public ReservaDto cancelar(Long id) {
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada con id: " + id));

        reserva.setEstadoPago(EstadoReserva.CANCELADA);
        Reserva reservaActualizada = reservaRepository.save(reserva);
        return convertirADto(reservaActualizada);
    }

    private ReservaDto convertirADto(Reserva reserva) {
        return ReservaDto.builder()
                .id(reserva.getId())
                .itinerarioId(reserva.getItinerario() != null ? reserva.getItinerario().getId() : null)
                .fechaReserva(reserva.getFechaReserva())
                .montoTotal(reserva.getMontoTotal())
                .estadoPago(reserva.getEstadoPago() != null ? reserva.getEstadoPago().name() : null)
                .build();
    }
}