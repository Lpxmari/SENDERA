package com.sendera.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/** Actividad turística de un destino; es lo que una persona agrega a su viaje y, más adelante, reserva. */
@Entity
@Table(name = "actividades")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Actividad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destino_id", nullable = false)
    private Destino destino;

    @Column(nullable = false, length = 180)
    private String nombre;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "duracion_minutos", nullable = false)
    private Integer duracionMinutos;

    @Column(nullable = false, length = 20)
    private String dificultad;

    @Column(name = "precio_por_persona", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioPorPersona;

    @Column(nullable = false)
    private Boolean accesibilidad;
}
