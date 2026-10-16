package com.sendera.service.impl;

import com.sendera.dto.CrearResenaDto;
import com.sendera.dto.ResenaDto;
import com.sendera.exception.ArchivoImagenInvalidoException;
import com.sendera.exception.RecursoNoEncontradoException;
import com.sendera.model.Resena;
import com.sendera.model.Usuario;
import com.sendera.repository.DestinoRepository;
import com.sendera.repository.ResenaRepository;
import com.sendera.repository.ResenaResumen;
import com.sendera.repository.UsuarioRepository;
import com.sendera.security.UsuarioActual;
import com.sendera.service.ResenaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ResenaServiceImpl implements ResenaService {

    private static final long TAMANO_MAXIMO_IMAGEN = 5L * 1024 * 1024;
    private static final Map<String, String> TIPOS_IMAGEN = Map.of(
            "image/jpeg", "image/jpeg",
            "image/png", "image/png",
            "image/webp", "image/webp"
    );

    private final ResenaRepository resenaRepository;
    private final DestinoRepository destinoRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ResenaDto> listarPorDestino(Long destinoId) {
        verificarDestino(destinoId);
        return resenaRepository.buscarResumenPorDestino(destinoId)
                .stream()
                .map(resena -> convertirADto(resena, destinoId))
                .toList();
    }

    @Override
    @Transactional
    public ResenaDto crear(Long destinoId, CrearResenaDto dto, MultipartFile archivo, UsuarioActual solicitante) {
        verificarDestino(destinoId);
        Usuario usuario = usuarioRepository.findById(solicitante.id())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado."));

        Resena.ResenaBuilder resena = Resena.builder()
                .destino(destinoRepository.getReferenceById(destinoId))
                .usuario(usuario)
                .comentario(dto.getComentario().trim())
                .calificacion(dto.getCalificacion());

        if (archivo != null && !archivo.isEmpty()) {
            validarYAdjuntarImagen(resena, archivo);
        }
        return convertirADto(resenaRepository.save(resena.build()));
    }

    @Override
    @Transactional(readOnly = true)
    public Resena obtenerImagen(Long destinoId, Long resenaId) {
        verificarDestino(destinoId);
        Resena resena = resenaRepository.findByIdAndDestinoId(resenaId, destinoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Imagen de reseña no encontrada."));
        if (resena.getImagen() == null) {
            throw new RecursoNoEncontradoException("La reseña no tiene una imagen adjunta.");
        }
        return resena;
    }

    private void verificarDestino(Long destinoId) {
        if (!destinoRepository.existsById(destinoId)) {
            throw new RecursoNoEncontradoException("Destino no encontrado con id: " + destinoId);
        }
    }

    private void validarYAdjuntarImagen(Resena.ResenaBuilder resena, MultipartFile archivo) {
        if (archivo.getSize() > TAMANO_MAXIMO_IMAGEN) {
            throw new ArchivoImagenInvalidoException("La imagen no puede superar los 5 MB.");
        }

        String tipoDeclarado = archivo.getContentType();
        String tipoNormalizado = tipoDeclarado != null ? tipoDeclarado.toLowerCase(Locale.ROOT) : "";
        if (!TIPOS_IMAGEN.containsKey(tipoNormalizado)) {
            throw new ArchivoImagenInvalidoException("Adjunta una imagen JPEG, PNG o WebP.");
        }

        try {
            byte[] bytes = archivo.getBytes();
            String tipoDetectado = detectarTipoImagen(bytes);
            if (!tipoNormalizado.equals(tipoDetectado)) {
                throw new ArchivoImagenInvalidoException("El contenido del archivo no coincide con su formato.");
            }
            resena.imagen(bytes).imagenContentType(TIPOS_IMAGEN.get(tipoDetectado));
        } catch (IOException ex) {
            throw new ArchivoImagenInvalidoException("No se pudo leer la imagen adjunta.");
        }
    }

    private String detectarTipoImagen(byte[] bytes) {
        if (bytes.length >= 3
                && (bytes[0] & 0xff) == 0xff
                && (bytes[1] & 0xff) == 0xd8
                && (bytes[2] & 0xff) == 0xff) {
            return "image/jpeg";
        }
        if (bytes.length >= 8
                && (bytes[0] & 0xff) == 0x89
                && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G'
                && (bytes[4] & 0xff) == 0x0d && (bytes[5] & 0xff) == 0x0a
                && (bytes[6] & 0xff) == 0x1a && (bytes[7] & 0xff) == 0x0a) {
            return "image/png";
        }
        if (bytes.length >= 12
                && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "image/webp";
        }
        throw new ArchivoImagenInvalidoException("El archivo adjunto no es una imagen JPEG, PNG o WebP válida.");
    }

    private ResenaDto convertirADto(Resena resena) {
        Usuario usuario = resena.getUsuario();
        return ResenaDto.builder()
                .id(resena.getId())
                .destinoId(resena.getDestino().getId())
                .comentario(resena.getComentario())
                .calificacion(resena.getCalificacion())
                .autor((usuario.getNombre() + " " + usuario.getApellidos()).trim())
                .creadaEn(resena.getCreadaEn())
                .imagenUrl(resena.getImagen() == null
                        ? null
                        : "/destinos/" + resena.getDestino().getId() + "/resenas/" + resena.getId() + "/imagen")
                .build();
    }

    private ResenaDto convertirADto(ResenaResumen resena, Long destinoId) {
        return ResenaDto.builder()
                .id(resena.getId())
                .destinoId(resena.getDestinoId())
                .comentario(resena.getComentario())
                .calificacion(resena.getCalificacion())
                .autor(resena.getAutor().trim())
                .creadaEn(resena.getCreadaEn())
                .imagenUrl(Boolean.TRUE.equals(resena.getTieneImagen())
                        ? "/destinos/" + destinoId + "/resenas/" + resena.getId() + "/imagen"
                        : null)
                .build();
    }
}
