package com.sendera.service.impl;

import com.sendera.dto.ActividadDto;
import com.sendera.exception.RecursoNoEncontradoException;
import com.sendera.model.Actividad;
import com.sendera.repository.ActividadRepository;
import com.sendera.service.ActividadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ActividadServiceImpl implements ActividadService {

    private final ActividadRepository actividadRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ActividadDto> listar(Long destinoId) {
        List<Actividad> actividades = destinoId == null
                ? actividadRepository.findTodas()
                : actividadRepository.findPorDestino(destinoId);
        return actividades.stream().map(ActividadServiceImpl::convertirADto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ActividadDto obtenerPorId(Long id) {
        return actividadRepository.findConDestinoById(id)
                .map(ActividadServiceImpl::convertirADto)
                .orElseThrow(() -> new RecursoNoEncontradoException("Actividad no encontrada con id: " + id));
    }

    static ActividadDto convertirADto(Actividad actividad) {
        return ActividadDto.builder()
                .id(actividad.getId())
                .destinoId(actividad.getDestino().getId())
                .destinoNombre(actividad.getDestino().getNombre())
                .ubicacion(actividad.getDestino().getUbicacion())
                .nombre(actividad.getNombre())
                .descripcion(actividad.getDescripcion())
                .duracionMinutos(actividad.getDuracionMinutos())
                .dificultad(actividad.getDificultad())
                .precioPorPersona(actividad.getPrecioPorPersona())
                .accesibilidad(actividad.getAccesibilidad())
                .build();
    }
}
