package com.sendera.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "usuarios")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 120)
    private String apellidos;

    @Column(nullable = false, unique = true, length = 160)
    private String correo;

    @Column(name = "contrasena_hash", nullable = false, length = 100)
    private String contrasenaHash;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @Column(nullable = false, length = 20)
    private String rol;

    /** false = acceso retirado por un administrador (la cuenta y sus reservas se conservan). */
    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

    // ---- Datos del perfil del viajero (NC-01). Opcionales: las cuentas creadas antes no los tienen ----

    @Column(name = "numero_identificacion", length = 15)
    private String numeroIdentificacion;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "pais_residencia", length = 80)
    private String paisResidencia;

    /** Texto libre, en el orden elegido por la persona; no son destinos del catálogo. */
    @ElementCollection
    @CollectionTable(name = "usuario_destinos_favoritos", joinColumns = @JoinColumn(name = "usuario_id", nullable = false))
    @OrderColumn(name = "orden")
    @Column(name = "destino", nullable = false, length = 80)
    @Builder.Default
    private List<String> destinosFavoritos = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "usuario_intereses", joinColumns = @JoinColumn(name = "usuario_id", nullable = false))
    @OrderColumn(name = "orden")
    @Column(name = "interes", nullable = false, length = 60)
    @Builder.Default
    private List<String> intereses = new ArrayList<>();

    @PrePersist
    void antesDePersistir() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }
        if (rol == null || rol.isBlank()) {
            rol = "ROLE_CLIENTE";
        }
    }
}
