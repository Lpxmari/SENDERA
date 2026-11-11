package com.sendera.service.impl;

import com.sendera.exception.RecursoNoEncontradoException;
import com.sendera.dto.CrearDestinoDto;
import com.sendera.dto.DestinoDto;
import com.sendera.dto.FiltroDestinoDto;
import com.sendera.dto.ItinerarioDto;
import com.sendera.dto.PaginaResultadosDto;
import com.sendera.exception.DestinoConItinerariosException;
import com.sendera.exception.FiltroInvalidoException;
import com.sendera.model.Destino;
import com.sendera.repository.DestinoRepository;
import com.sendera.repository.DestinoSpecifications;
import com.sendera.repository.ItinerarioRepository;
import com.sendera.security.UsuarioActual;
import com.sendera.service.AuditoriaService;
import com.sendera.service.DestinoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DestinoServiceImpl implements DestinoService {

    private static final Set<String> DIFICULTADES = Set.of("BAJA", "MODERADA", "ALTA");

    private final DestinoRepository destinoRepository;
    private final ItinerarioRepository itinerarioRepository;
    private final AuditoriaService auditoriaService;

    @Override
    @Transactional(readOnly = true)
    public PaginaResultadosDto<DestinoDto> listarDestinos(FiltroDestinoDto filtro, int pagina, int tamano) {
        FiltroDestinoDto f = filtro != null ? filtro : new FiltroDestinoDto();
        validarFiltro(f);
        if (pagina < 0) {
            throw new FiltroInvalidoException("El número de página debe ser 0 o mayor.");
        }
        if (tamano < 1) {
            throw new FiltroInvalidoException("El tamaño de página debe ser al menos 1.");
        }
        int tamanoAplicado = Math.min(tamano, TAMANO_MAXIMO);

        // La paginación se resuelve en la base de datos (LIMIT/OFFSET + COUNT), no en memoria.
        Page<Destino> resultado = destinoRepository.findAll(
                DestinoSpecifications.conFiltros(f),
                PageRequest.of(pagina, tamanoAplicado, Sort.by(Sort.Direction.ASC, "id")));
        return PaginaResultadosDto.<DestinoDto>builder()
                .contenido(resultado.getContent().stream().map(this::convertirADto).collect(Collectors.toList()))
                .pagina(pagina)
                .tamano(tamanoAplicado)
                .totalElementos(resultado.getTotalElements())
                .totalPaginas(resultado.getTotalPages())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public DestinoDto obtenerDestinoPorId(Long id) {
        return convertirADto(buscarEntidad(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItinerarioDto> listarItinerariosDisponibles(Long destinoId) {
        buscarEntidad(destinoId);
        return itinerarioRepository
                .findByDestinoIdAndCuposDisponiblesGreaterThanAndFechaFinGreaterThanEqualOrderByFechaInicioAsc(
                        destinoId, 0, LocalDate.now())
                .stream()
                .map(i -> ItinerarioDto.builder()
                        .id(i.getId())
                        .nombre(i.getNombre())
                        .fechaInicio(i.getFechaInicio())
                        .fechaFin(i.getFechaFin())
                        .precioBase(i.getPrecioBase())
                        .cuposDisponibles(i.getCuposDisponibles())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public DestinoDto crearDestino(CrearDestinoDto dto, UsuarioActual actor) {
        Destino destino = new Destino();
        aplicarDatos(destino, dto);
        destino = destinoRepository.save(destino);
        auditoriaService.registrar(actor, "DESTINO_CREAR", "destino", destino.getId(), destino.getNombre());
        return convertirADto(destino);
    }

    @Override
    @Transactional
    public DestinoDto actualizarDestino(Long id, CrearDestinoDto dto, UsuarioActual actor) {
        Destino destino = buscarEntidad(id);
        aplicarDatos(destino, dto);
        destino = destinoRepository.save(destino);
        auditoriaService.registrar(actor, "DESTINO_ACTUALIZAR", "destino", id, destino.getNombre());
        return convertirADto(destino);
    }

    @Override
    @Transactional
    public void eliminarDestino(Long id, UsuarioActual actor) {
        Destino destino = buscarEntidad(id);
        if (itinerarioRepository.existsByDestinoId(id)) {
            throw new DestinoConItinerariosException();
        }
        String nombre = destino.getNombre();
        destinoRepository.delete(destino);
        auditoriaService.registrar(actor, "DESTINO_ELIMINAR", "destino", id, nombre);
    }

    // ------------------------------------------------------------------ filtros

    private void validarFiltro(FiltroDestinoDto f) {
        if (f.getDificultad() != null && !f.getDificultad().isBlank()) {
            String dificultad = f.getDificultad().trim().toUpperCase(Locale.ROOT);
            if (!DIFICULTADES.contains(dificultad)) {
                throw new FiltroInvalidoException("La dificultad debe ser BAJA, MODERADA o ALTA.");
            }
            f.setDificultad(dificultad);
        }
        if ((f.getPrecioMin() != null && f.getPrecioMin().signum() < 0)
                || (f.getPrecioMax() != null && f.getPrecioMax().signum() < 0)) {
            throw new FiltroInvalidoException("Los precios no pueden ser negativos.");
        }
        if (f.getPrecioMin() != null && f.getPrecioMax() != null
                && f.getPrecioMin().compareTo(f.getPrecioMax()) > 0) {
            throw new FiltroInvalidoException("El precio mínimo no puede ser mayor que el máximo.");
        }
    }

    // ------------------------------------------------------------------ utilidades

    private Destino buscarEntidad(Long id) {
        return destinoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Destino no encontrado con id: " + id));
    }

    private void aplicarDatos(Destino destino, CrearDestinoDto dto) {
        destino.setNombre(dto.getNombre().trim());
        destino.setDescripcion(dto.getDescripcion().trim());
        destino.setUbicacion(dto.getUbicacion().trim());
        destino.setPrecioBase(dto.getPrecioBase());
        // --- AÑADIDO PROCESO 2 ---
        destino.setDificultad(dto.getDificultad() != null ? dto.getDificultad().trim() : null);
        destino.setAccesibilidad(dto.getAccesibilidad());
        destino.setFotosUrl(normalizarFotos(dto.getFotosUrl()));
    }

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
        // --- AÑADIDO PROCESO 2 ---
        dto.setDificultad(destino.getDificultad());
        dto.setAccesibilidad(destino.getAccesibilidad());
        dto.setFotosUrl(destino.getFotosUrl() != null ? new ArrayList<>(destino.getFotosUrl()) : new ArrayList<>());
        return dto;
    }
}