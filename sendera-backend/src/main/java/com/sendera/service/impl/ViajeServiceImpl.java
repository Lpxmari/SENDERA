package com.sendera.service.impl;

import com.sendera.dto.GuardarViajeDto;
import com.sendera.dto.ViajeActividadDto;
import com.sendera.dto.ViajeActividadEntradaDto;
import com.sendera.dto.ViajeDto;
import com.sendera.dto.ViajeResumenDto;
import com.sendera.exception.HorarioSolapadoException;
import com.sendera.exception.RecursoNoEncontradoException;
import com.sendera.exception.ViajeInvalidoException;
import com.sendera.model.Actividad;
import com.sendera.model.Viaje;
import com.sendera.model.ViajeActividad;
import com.sendera.repository.ActividadRepository;
import com.sendera.repository.UsuarioRepository;
import com.sendera.repository.ViajeRepository;
import com.sendera.security.UsuarioActual;
import com.sendera.service.ViajeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ViajeServiceImpl implements ViajeService {

    /** "Hoy" es el de Colombia: el servidor corre en UTC y de noche iría un día adelantado. */
    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final int MINUTOS_POR_DIA = 24 * 60;
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final ViajeRepository viajeRepository;
    private final ActividadRepository actividadRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ViajeResumenDto> listar(UsuarioActual solicitante) {
        return viajeRepository.findConActividadesByUsuarioId(solicitante.id()).stream()
                .map(this::convertirAResumen)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ViajeDto obtenerPorId(Long id, UsuarioActual solicitante) {
        return convertirADto(buscarPropio(id, solicitante));
    }

    @Override
    @Transactional
    public ViajeDto crear(GuardarViajeDto dto, UsuarioActual solicitante) {
        validarFechas(dto, null);
        Viaje viaje = Viaje.builder()
                // El propietario sale del JWT, nunca del cuerpo de la petición
                .usuario(usuarioRepository.getReferenceById(solicitante.id()))
                .build();
        aplicar(viaje, dto);
        return convertirADto(viajeRepository.save(viaje));
    }

    @Override
    @Transactional
    public ViajeDto actualizar(Long id, GuardarViajeDto dto, UsuarioActual solicitante) {
        Viaje viaje = buscarPropio(id, solicitante);
        validarFechas(dto, viaje.getFechaInicio());
        aplicar(viaje, dto);
        return convertirADto(viajeRepository.save(viaje));
    }

    @Override
    @Transactional
    public void eliminar(Long id, UsuarioActual solicitante) {
        viajeRepository.delete(buscarPropio(id, solicitante));
    }

    /** Solo el propietario; para cualquier otra persona el viaje "no existe" (404). */
    private Viaje buscarPropio(Long id, UsuarioActual solicitante) {
        return viajeRepository.findConActividadesByIdAndUsuarioId(id, solicitante.id())
                .orElseThrow(() -> new RecursoNoEncontradoException("Itinerario no encontrado con id: " + id));
    }

    private void validarFechas(GuardarViajeDto dto, LocalDate fechaInicioActual) {
        if (dto.getFechaFin().isBefore(dto.getFechaInicio())) {
            throw new ViajeInvalidoException("La fecha de finalización no puede ser anterior a la de inicio.");
        }
        // Un viaje ya iniciado puede editarse sin mover su fecha de inicio; cambiarla al pasado no.
        boolean inicioCambia = fechaInicioActual == null || !fechaInicioActual.equals(dto.getFechaInicio());
        if (inicioCambia && dto.getFechaInicio().isBefore(LocalDate.now(ZONA))) {
            throw new ViajeInvalidoException("No puedes usar fechas pasadas. Elige una fecha de inicio desde hoy.");
        }
    }

    private void aplicar(Viaje viaje, GuardarViajeDto dto) {
        List<ViajeActividadEntradaDto> entradas = dto.getActividades() != null ? dto.getActividades() : List.of();
        Map<Long, Actividad> catalogo = cargarCatalogo(entradas);
        validarActividades(dto, entradas, catalogo);

        viaje.setNombre(dto.getNombre().trim());
        viaje.setFechaInicio(dto.getFechaInicio());
        viaje.setFechaFin(dto.getFechaFin());

        // Se reemplaza la lista completa. La hora define el orden: la posición dentro de cada día es la cronológica,
        // sin importar en qué orden llegue la lista (no puede haber empates: dos actividades a la misma hora se solapan).
        List<ViajeActividadEntradaDto> cronologicas = entradas.stream()
                .sorted(Comparator.comparing(ViajeActividadEntradaDto::getFecha)
                        .thenComparing(ViajeActividadEntradaDto::getHoraInicio))
                .toList();
        viaje.getActividades().clear();
        Map<LocalDate, Integer> posicionPorDia = new HashMap<>();
        for (ViajeActividadEntradaDto entrada : cronologicas) {
            int orden = posicionPorDia.merge(entrada.getFecha(), 1, Integer::sum);
            viaje.getActividades().add(ViajeActividad.builder()
                    .viaje(viaje)
                    .actividad(catalogo.get(entrada.getActividadId()))
                    .fecha(entrada.getFecha())
                    .horaInicio(entrada.getHoraInicio())
                    .orden(orden)
                    .build());
        }
    }

    private Map<Long, Actividad> cargarCatalogo(List<ViajeActividadEntradaDto> entradas) {
        Set<Long> ids = entradas.stream().map(ViajeActividadEntradaDto::getActividadId).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, Actividad> catalogo = actividadRepository.findConDestinoByIdIn(ids).stream()
                .collect(Collectors.toMap(Actividad::getId, a -> a));
        Set<Long> faltantes = new HashSet<>(ids);
        faltantes.removeAll(catalogo.keySet());
        if (!faltantes.isEmpty()) {
            throw new RecursoNoEncontradoException("Actividad no encontrada con id: " + faltantes.iterator().next());
        }
        return catalogo;
    }

    private void validarActividades(GuardarViajeDto dto, List<ViajeActividadEntradaDto> entradas, Map<Long, Actividad> catalogo) {
        for (ViajeActividadEntradaDto entrada : entradas) {
            Actividad actividad = catalogo.get(entrada.getActividadId());
            if (entrada.getFecha().isBefore(dto.getFechaInicio()) || entrada.getFecha().isAfter(dto.getFechaFin())) {
                throw new ViajeInvalidoException("«" + actividad.getNombre() + "» está fuera de las fechas del viaje ("
                        + FORMATO_FECHA.format(dto.getFechaInicio()) + " a " + FORMATO_FECHA.format(dto.getFechaFin()) + ").");
            }
            if (minutosDelDia(entrada.getHoraInicio()) + actividad.getDuracionMinutos() > MINUTOS_POR_DIA) {
                throw new ViajeInvalidoException("«" + actividad.getNombre()
                        + "» terminaría después de la medianoche. Elige una hora de inicio más temprana.");
            }
        }
        validarSolapes(entradas, catalogo);
    }

    /** Dos actividades del mismo día se solapan si una empieza antes de que termine la otra; terminar justo al empezar no cuenta. */
    private void validarSolapes(List<ViajeActividadEntradaDto> entradas, Map<Long, Actividad> catalogo) {
        Map<LocalDate, List<ViajeActividadEntradaDto>> porDia = new TreeMap<>();
        for (ViajeActividadEntradaDto entrada : entradas) {
            porDia.computeIfAbsent(entrada.getFecha(), d -> new java.util.ArrayList<>()).add(entrada);
        }
        for (Map.Entry<LocalDate, List<ViajeActividadEntradaDto>> dia : porDia.entrySet()) {
            List<ViajeActividadEntradaDto> ordenadas = dia.getValue().stream()
                    .sorted(Comparator.comparing(ViajeActividadEntradaDto::getHoraInicio))
                    .toList();
            ViajeActividadEntradaDto anterior = null;
            int finAnterior = 0;
            for (ViajeActividadEntradaDto actual : ordenadas) {
                int inicio = minutosDelDia(actual.getHoraInicio());
                if (anterior != null && inicio < finAnterior) {
                    throw new HorarioSolapadoException(descripcionSolape(dia.getKey(), anterior, actual, catalogo));
                }
                int fin = inicio + catalogo.get(actual.getActividadId()).getDuracionMinutos();
                if (anterior == null || fin > finAnterior) {
                    anterior = actual;
                    finAnterior = fin;
                }
            }
        }
    }

    private String descripcionSolape(LocalDate dia, ViajeActividadEntradaDto a, ViajeActividadEntradaDto b, Map<Long, Actividad> catalogo) {
        return "Las actividades " + rango(a, catalogo) + " y " + rango(b, catalogo)
                + " se solapan el " + FORMATO_FECHA.format(dia) + ". Cambia el horario de una de ellas.";
    }

    private String rango(ViajeActividadEntradaDto entrada, Map<Long, Actividad> catalogo) {
        Actividad actividad = catalogo.get(entrada.getActividadId());
        LocalTime fin = entrada.getHoraInicio().plusMinutes(actividad.getDuracionMinutos());
        return "«" + actividad.getNombre() + "» (" + FORMATO_HORA.format(entrada.getHoraInicio()) + "–" + FORMATO_HORA.format(fin) + ")";
    }

    private static int minutosDelDia(LocalTime hora) {
        return hora.getHour() * 60 + hora.getMinute();
    }

    private ViajeResumenDto convertirAResumen(Viaje viaje) {
        List<ViajeActividad> actividades = ordenadas(viaje);
        return ViajeResumenDto.builder()
                .id(viaje.getId())
                .nombre(viaje.getNombre())
                .ubicacion(actividades.isEmpty() ? null : actividades.get(0).getActividad().getDestino().getUbicacion())
                .fechaInicio(viaje.getFechaInicio())
                .fechaFin(viaje.getFechaFin())
                .totalActividades(actividades.size())
                .primeraHora(actividades.isEmpty() ? null : actividades.get(0).getHoraInicio())
                .build();
    }

    private ViajeDto convertirADto(Viaje viaje) {
        return ViajeDto.builder()
                .id(viaje.getId())
                .nombre(viaje.getNombre())
                .fechaInicio(viaje.getFechaInicio())
                .fechaFin(viaje.getFechaFin())
                .actividades(ordenadas(viaje).stream().map(ViajeServiceImpl::convertirActividad).toList())
                .build();
    }

    private static List<ViajeActividad> ordenadas(Viaje viaje) {
        return viaje.getActividades().stream()
                .sorted(Comparator.comparing(ViajeActividad::getFecha).thenComparing(ViajeActividad::getOrden))
                .toList();
    }

    private static ViajeActividadDto convertirActividad(ViajeActividad va) {
        Actividad actividad = va.getActividad();
        return ViajeActividadDto.builder()
                .id(va.getId())
                .actividadId(actividad.getId())
                .destinoId(actividad.getDestino().getId())
                .nombre(actividad.getNombre())
                .ubicacion(actividad.getDestino().getUbicacion())
                .fecha(va.getFecha())
                .horaInicio(va.getHoraInicio())
                .duracionMinutos(actividad.getDuracionMinutos())
                .dificultad(actividad.getDificultad())
                .orden(va.getOrden())
                .build();
    }
}
