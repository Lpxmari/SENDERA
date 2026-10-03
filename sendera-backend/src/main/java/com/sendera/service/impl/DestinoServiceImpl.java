package com.sendera.service.impl;

import com.sendera.dto.CrearDestinoDto;
import com.sendera.dto.DestinoDto;
import com.sendera.model.Destino;
import com.sendera.repository.DestinoRepository;
import com.sendera.service.DestinoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DestinoServiceImpl implements DestinoService {

    private final DestinoRepository destinoRepository;

    @Override
    public List<DestinoDto> listarTodos() {
        return destinoRepository.findAll().stream()
                .map(this::convertirADto)
                .collect(Collectors.toList());
    }

    @Override
    public DestinoDto obtenerPorId(Long id) {
        Destino destino = destinoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Destino no encontrado con id: " + id));
        return convertirADto(destino);
    }

    @Override
    public DestinoDto crear(CrearDestinoDto dto) {
        Destino destino = Destino.builder()
                .nombre(dto.getNombre())
                .descripcion(dto.getDescripcion())
                .ubicacion(dto.getUbicacion())
                .precioBase(dto.getPrecioBase())
                .fotosUrl(dto.getFotosUrl())
                .build();

        Destino destinoGuardado = destinoRepository.save(destino);
        return convertirADto(destinoGuardado);
    }

    private DestinoDto convertirADto(Destino destino) {
        DestinoDto dto = new DestinoDto();
        dto.setId(destino.getId());
        dto.setNombre(destino.getNombre());
        dto.setDescripcion(destino.getDescripcion());
        dto.setUbicacion(destino.getUbicacion());
        dto.setPrecioBase(destino.getPrecioBase());
        dto.setFotosUrl(destino.getFotosUrl());
        return dto;
    }
}