package com.sendera.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Reserva con los datos que el administrador necesita para gestionarla (RF-013). */
@Data
@Builder
public class ReservaAdminDto {
    private Long id;
    private Long usuarioId;
    private String usuarioNombre;
    private String usuarioCorreo;
    private Long itinerarioId;
    private String itinerarioNombre;
    private String destinoNombre;
    private LocalDate fechaReserva;
    private BigDecimal montoTotal;
    private String estadoReserva;
}
