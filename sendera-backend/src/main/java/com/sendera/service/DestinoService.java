package com.sendera.service;

import com.sendera.dto.CrearDestinoDto;
import com.sendera.dto.DestinoDto;
import com.sendera.dto.FiltroDestinoDto;

import java.util.List;

public interface DestinoService {
    List<DestinoDto> listarTodos();
    List<DestinoDto> buscar(FiltroDestinoDto filtro);
    DestinoDto obtenerPorId(Long id);
    DestinoDto crear(CrearDestinoDto dto);
    DestinoDto actualizar(Long id, CrearDestinoDto dto);
    void eliminar(Long id);
}
