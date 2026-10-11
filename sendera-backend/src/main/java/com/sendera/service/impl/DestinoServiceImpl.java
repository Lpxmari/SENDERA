package com.sendera.service.impl;

import com.sendera.dto.CrearDestinoDto;
import com.sendera.dto.DestinoDto;
import com.sendera.dto.FiltroDestinoDto;
import com.sendera.exception.DestinoConItinerariosException;
import com.sendera.exception.FiltroInvalidoException;
import com.sendera.model.Destino;
import com.sendera.model.Itinerario;
import com.sendera.repository.DestinoRepository;
import com.sendera.repository.DestinoSpecifications;
import com.sendera.repository.ItinerarioRepository;
import com.sendera.service.DestinoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DestinoServiceImpl implements DestinoService {

    private final DestinoRepository destinoRepository;
    private final ItinerarioRepository itinerarioRepository;

    @Override
    @Transactional(readOnly = true)
    public List<DestinoDto> listarTodos() {
        return buscar(new FiltroDestinoDto());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DestinoDto> buscar(FiltroDestinoDto filtro) {
        FiltroDestinoDto f = filtro != null ? filtro : new FiltroDestinoDto();
        validarFiltro(f);

        List<Destino> destinos = destinoRepository.findAll(
                DestinoSpecifications.conFiltros(f), Sort.by(Sort.Direction.ASC, "id"));

        if (f.getDuracionMin() != null || f.getDuracionMax() != null) {
            destinos = filtrarPorDuracion(destinos, f);
        }
        return destinos.stream().map(this::convertirADto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DestinoDto obtenerPorId(Long id) {
        return convertirADto(buscarEntidad(id));
    }

    @Override
    @Transactional
    public DestinoDto crear(CrearDestinoDto dto) {
        Destino destino = new Destino();
        aplicarDatos(destino, dto);
        return convertirADto(destinoRepository.save(destino));
    }

    @Override
    @Transactional
    public DestinoDto actualizar(Long id, CrearDestinoDto dto) {
        Destino destino = buscarEntidad(id);
        aplicarDatos(destino, dto);
        return convertirADto(destinoRepository.save(destino));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Destino destino = buscarEntidad(id);
        // La BD usa ON DELETE RESTRICT: se avisa con un 409 claro en vez de dejar que falle la FK.
        if (itinerarioRepository.existsByDestinoId(id)) {
            throw new DestinoConItinerariosException();
        }
        destinoRepository.delete(destino);
    }

    // ------------------------------------------------------------------ filtros

    private void validarFiltro(FiltroDestinoDto f) {
        if ((f.getPrecioMin() != null && f.getPrecioMin().signum() < 0)
                || (f.getPrecioMax() != null && f.getPrecioMax().signum() < 0)) {
            throw new FiltroInvalidoException("Los precios no pueden ser negativos.");
        }
        if (f.getPrecioMin() != null && f.getPrecioMax() != null
                && f.getPrecioMin().compareTo(f.getPrecioMax()) > 0) {
            throw new FiltroInvalidoException("El precio mínimo no puede ser mayor que el máximo.");
        }
        if ((f.getDuracionMin() != null && f.getDuracionMin() < 1)
                || (f.getDuracionMax() != null && f.getDuracionMax() < 1)) {
            throw new FiltroInvalidoException("La duración debe ser de al menos 1 día.");
        }
        if (f.getDuracionMin() != null && f.getDuracionMax() != null
                && f.getDuracionMin() > f.getDuracionMax()) {
            throw new FiltroInvalidoException("La duración mínima no puede ser mayor que la máxima.");
        }
    }

    /**
     * La duración vive en los itinerarios (fecha inicio/fin), no en el destino. Un destino cumple si
     * al menos uno de sus itinerarios cae en el rango; sin itinerarios no cumple.
     */
    private List<Destino> filtrarPorDuracion(List<Destino> destinos, FiltroDestinoDto f) {
        if (destinos.isEmpty()) {
            return destinos;
        }
        List<Long> ids = destinos.stream().map(Destino::getId).collect(Collectors.toList());
        Map<Long, List<Itinerario>> porDestino = itinerarioRepository.findByDestinoIdIn(ids).stream()
                .collect(Collectors.groupingBy(i -> i.getDestino().getId()));

        return destinos.stream()
                .filter(d -> porDestino.getOrDefault(d.getId(), List.of()).stream()
                        .anyMatch(i -> cumpleDuracion(duracionEnDias(i), f)))
                .collect(Collectors.toList());
    }

    private boolean cumpleDuracion(long dias, FiltroDestinoDto f) {
        return (f.getDuracionMin() == null || dias >= f.getDuracionMin())
                && (f.getDuracionMax() == null || dias <= f.getDuracionMax());
    }

    private long duracionEnDias(Itinerario itinerario) {
        return ChronoUnit.DAYS.between(itinerario.getFechaInicio(), itinerario.getFechaFin()) + 1;
    }

    // ------------------------------------------------------------------ utilidades

    private Destino buscarEntidad(Long id) {
        return destinoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Destino no encontrado con id: " + id));
    }

    private void aplicarDatos(Destino destino, CrearDestinoDto dto) {
        destino.setNombre(dto.getNombre().trim());
        destino.setDescripcion(dto.getDescripcion().trim());
        destino.setUbicacion(dto.getUbicacion().trim());
        destino.setPrecioBase(dto.getPrecioBase());
        destino.setFotosUrl(normalizarFotos(dto.getFotosUrl()));
    }

    /** Sin vacíos ni repetidas: la tabla destino_fotos tiene UNIQUE (destino_id, foto_url). */
    private List<String> normalizarFotos(List<String> fotos) {
        if (fotos == null) {
            return new ArrayList<>();
        }
        return fotos.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(url -> !url.isEmpty())
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private DestinoDto convertirADto(Destino destino) {
        DestinoDto dto = new DestinoDto();
        dto.setId(destino.getId());
        dto.setNombre(destino.getNombre());
        dto.setDescripcion(destino.getDescripcion());
        dto.setUbicacion(destino.getUbicacion());
        dto.setPrecioBase(destino.getPrecioBase());
        dto.setFotosUrl(destino.getFotosUrl() != null ? new ArrayList<>(destino.getFotosUrl()) : new ArrayList<>());
        return dto;
    }
}
