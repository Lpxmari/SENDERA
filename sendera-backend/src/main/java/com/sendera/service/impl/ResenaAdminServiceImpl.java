package com.sendera.service.impl;

import com.sendera.dto.PaginaDto;
import com.sendera.dto.ResenaAdminDto;
import com.sendera.exception.FiltroInvalidoException;
import com.sendera.exception.RecursoNoEncontradoException;
import com.sendera.model.Resena;
import com.sendera.repository.ResenaAdminResumen;
import com.sendera.repository.ResenaRepository;
import com.sendera.security.UsuarioActual;
import com.sendera.service.AuditoriaService;
import com.sendera.service.ResenaAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ResenaAdminServiceImpl implements ResenaAdminService {

    private static final int TAMANO_MAXIMO = 100;

    private final ResenaRepository resenaRepository;
    private final AuditoriaService auditoriaService;

    @Override
    @Transactional(readOnly = true)
    public PaginaDto<ResenaAdminDto> listar(Long destinoId, Integer calificacionMax, String q, int pagina, int tamano) {
        if (pagina < 0 || tamano < 1 || tamano > TAMANO_MAXIMO) {
            throw new FiltroInvalidoException("La página debe ser 0 o mayor y el tamaño estar entre 1 y " + TAMANO_MAXIMO + ".");
        }
        if (calificacionMax != null && (calificacionMax < 1 || calificacionMax > 5)) {
            throw new FiltroInvalidoException("La calificación máxima debe estar entre 1 y 5.");
        }
        // El orden se fija en la consulta de proyección por alias; aquí solo se pagina.
        Page<ResenaAdminResumen> resultado = resenaRepository.buscarAdmin(destinoId, calificacionMax, patron(q),
                PageRequest.of(pagina, tamano, Sort.by(Sort.Direction.DESC, "creadaEn").and(Sort.by(Sort.Direction.DESC, "id"))));
        return PaginaDto.<ResenaAdminDto>builder()
                .contenido(resultado.getContent().stream().map(this::convertir).toList())
                .pagina(pagina)
                .tamano(tamano)
                .total(resultado.getTotalElements())
                .build();
    }

    @Override
    @Transactional
    public void eliminar(Long id, UsuarioActual actor) {
        Resena resena = resenaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reseña no encontrada con id: " + id));
        Long destinoId = resena.getDestino().getId();
        Long autorId = resena.getUsuario().getId();
        resenaRepository.delete(resena);
        // Solo identificadores: el texto de la reseña no se copia a la auditoría.
        auditoriaService.registrar(actor, "RESENA_ELIMINAR", "resena", id, "destino=" + destinoId + ", autor=" + autorId);
    }

    private String patron(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        return "%" + q.trim().toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }

    private ResenaAdminDto convertir(ResenaAdminResumen r) {
        return ResenaAdminDto.builder()
                .id(r.getId())
                .destinoId(r.getDestinoId())
                .destinoNombre(r.getDestinoNombre())
                .usuarioId(r.getUsuarioId())
                .autor(r.getAutor())
                .comentario(r.getComentario())
                .calificacion(r.getCalificacion())
                .tieneImagen(Boolean.TRUE.equals(r.getTieneImagen()))
                .creadaEn(r.getCreadaEn())
                .build();
    }
}
