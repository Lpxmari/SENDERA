package com.sendera.service;

import com.sendera.dto.CrearDestinoDto;
import com.sendera.dto.DestinoDto;
import java.util.List;

public interface DestinoService {
    List<DestinoDto> listarTodos();
    DestinoDto obtenerPorId(Long id);
    DestinoDto crear(CrearDestinoDto dto);
}