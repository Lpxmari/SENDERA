package com.sendera.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "destinos")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Destino {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false)
    private String ubicacion;

    @Column(nullable = false)
    private String dificultad;

    @Column(nullable = false)
    private Boolean accesibilidad;

    @Column(name = "precio_base", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioBase;

    @ElementCollection
    @CollectionTable(name = "destino_fotos", joinColumns = @JoinColumn(name = "destino_id", nullable = false))
    @Column(name = "foto_url", nullable = false)
    private List<String> fotosUrl;
}