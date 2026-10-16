package com.sendera.service;

import com.sendera.dto.CrearResenaDto;
import com.sendera.dto.ResenaDto;
import com.sendera.model.Resena;
import com.sendera.security.UsuarioActual;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ResenaService {

    List<ResenaDto> listarPorDestino(Long destinoId);

    ResenaDto crear(Long destinoId, CrearResenaDto dto, MultipartFile imagen, UsuarioActual solicitante);

    Resena obtenerImagen(Long destinoId, Long resenaId);
}
