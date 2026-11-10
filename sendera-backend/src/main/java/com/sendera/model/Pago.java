package com.sendera.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Pago de una reserva procesado por Mercado Pago (Checkout Pro). SENDERA solo guarda referencias:
 * los datos de la tarjeta nunca pasan por el backend (RNF-06).
 */
@Entity
@Table(name = "pagos")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Un pago por reserva: cada nuevo intento reutiliza la fila y actualiza la preferencia. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reserva_id", nullable = false, unique = true)
    private Reserva reserva;

    @Column(name = "preferencia_id", nullable = false, length = 80)
    private String preferenciaId;

    /** Identificador del pago en Mercado Pago; se conoce cuando el comprador intenta pagar. */
    @Column(name = "pago_externo_id", length = 40)
    private String pagoExternoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_pago", nullable = false, length = 20)
    private EstadoPago estadoPago;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}
