package com.sendera.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Token de un solo uso para restablecer la contraseña. Solo se guarda su hash SHA-256. */
@Entity
@Table(name = "tokens_recuperacion")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TokenRecuperacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expira_en", nullable = false)
    private LocalDateTime expiraEn;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    void antesDePersistir() {
        if (creadoEn == null) {
            creadoEn = LocalDateTime.now();
        }
    }
}
