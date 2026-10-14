package com.sendera.service;

import com.sendera.dto.CrearDestinoDto;
import com.sendera.dto.DestinoDto;
import com.sendera.dto.FiltroDestinoDto;

import java.util.List;

public interface DestinoService {
    List<DestinoDto> listarDestinos(FiltroDestinoDto filtro);
    DestinoDto obtenerDestinoPorId(Long id);
    DestinoDto crearDestino(CrearDestinoDto dto);
    DestinoDto actualizarDestino(Long id, CrearDestinoDto dto);
    void eliminarDestino(Long id);
}