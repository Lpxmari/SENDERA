USE sendera;

CREATE TABLE IF NOT EXISTS usuarios (
                                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                        nombre VARCHAR(100) NOT NULL,
    apellidos VARCHAR(120) NOT NULL,
    correo VARCHAR(160) NOT NULL,
    contrasena_hash VARCHAR(100) NOT NULL,
    fecha_registro DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_usuarios_correo (correo)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tokens_recuperacion (
                                                   id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                   usuario_id BIGINT NOT NULL,
                                                   token_hash VARCHAR(64) NOT NULL,
    expira_en DATETIME NOT NULL,
    creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_tokens_recuperacion_hash (token_hash),
    INDEX idx_tokens_recuperacion_usuario (usuario_id),
    CONSTRAINT fk_tokens_recuperacion_usuario
    FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
    ON DELETE CASCADE ON UPDATE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
