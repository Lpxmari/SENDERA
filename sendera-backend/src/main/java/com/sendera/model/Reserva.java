package com.sendera.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "reservas")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate fechaInicio;

    @Column(nullable = false)
    private LocalDate fechaFin;

    @Column(nullable = false)
    private Integer cantidadPersonas;

    @Column(nullable = false)
    private BigDecimal precioTotal;

    @Column(nullable = false)
    private String estado; // PENDIENTE, CONFIRMADA, CANCELADA

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destino_id", nullable = false)
    private Destino destino;
}