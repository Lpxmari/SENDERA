package com.sendera.service;

import com.sendera.dto.CrearReservaDto;
import com.sendera.dto.ReservaDto;
import java.util.List;

public interface ReservaService {
    List<ReservaDto> listarTodas();
    ReservaDto obtenerPorId(Long id);
    ReservaDto crear(CrearReservaDto dto);
    ReservaDto cancelar(Long id);
}