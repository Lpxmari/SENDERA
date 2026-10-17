package com.sendera.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Acción administrativa registrada (RF-013). Nunca contiene contraseñas, tokens ni datos sensibles. */
@Entity
@Table(name = "auditoria_admin")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AuditoriaAdmin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nulo si la cuenta del administrador se eliminó después. */
    @Column(name = "admin_id")
    private Long adminId;

    @Column(nullable = false, length = 60)
    private String accion;

    @Column(nullable = false, length = 40)
    private String recurso;

    @Column(name = "recurso_id")
    private Long recursoId;

    @Column(length = 500)
    private String detalle;

    @Column(name = "creada_en", nullable = false, updatable = false)
    private LocalDateTime creadaEn;

    @PrePersist
    void antesDePersistir() {
        if (creadaEn == null) {
            creadaEn = LocalDateTime.now();
        }
    }
}
