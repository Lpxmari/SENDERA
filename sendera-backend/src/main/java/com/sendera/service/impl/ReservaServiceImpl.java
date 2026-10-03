package com.sendera.service.impl;

import com.sendera.dto.CrearReservaDto;
import com.sendera.dto.ReservaDto;
import com.sendera.model.Destino;
import com.sendera.model.Reserva;
import com.sendera.repository.DestinoRepository;
import com.sendera.repository.ReservaRepository;
import com.sendera.service.ReservaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservaServiceImpl implements ReservaService {

    private final ReservaRepository reservaRepository;
    private final DestinoRepository destinoRepository;

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
        Destino destino = destinoRepository.findById(dto.getDestinoId())
                .orElseThrow(() -> new RuntimeException("Destino no encontrado con id: " + dto.getDestinoId()));

        long dias = ChronoUnit.DAYS.between(dto.getFechaInicio(), dto.getFechaFin());
        if (dias <= 0) {
            throw new RuntimeException("La fecha de fin debe ser posterior a la fecha de inicio");
        }

        BigDecimal precioTotal = destino.getPrecioBase()
                .multiply(BigDecimal.valueOf(dias))
                .multiply(BigDecimal.valueOf(dto.getCantidadPersonas()));

        Reserva reserva = Reserva.builder()
                .destino(destino)
                .fechaInicio(dto.getFechaInicio())
                .fechaFin(dto.getFechaFin())
                .cantidadPersonas(dto.getCantidadPersonas())
                .precioTotal(precioTotal)
                .estado("CONFIRMADA")
                .build();

        Reserva reservaGuardada = reservaRepository.save(reserva);
        return convertirADto(reservaGuardada);
    }

    @Override
    public ReservaDto cancelar(Long id) {
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada con id: " + id));

        reserva.setEstado("CANCELADA");
        Reserva reservaActualizada = reservaRepository.save(reserva);
        return convertirADto(reservaActualizada);
    }

    private ReservaDto convertirADto(Reserva reserva) {
        return ReservaDto.builder()
                .id(reserva.getId())
                .destinoId(reserva.getDestino().getId())
                .nombreDestino(reserva.getDestino().getNombre())
                .fechaInicio(reserva.getFechaInicio())
                .fechaFin(reserva.getFechaFin())
                .cantidadPersonas(reserva.getCantidadPersonas())
                .precioTotal(reserva.getPrecioTotal())
                .estado(reserva.getEstado())
                .build();
    }
}