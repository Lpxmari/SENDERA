-- REFERENCIA del esquema de base de datos de SENDERA (MER aprobado, docs/04-modelo-entidad-relacion.md).
-- No se usan migraciones: este es el único script de creación. Para aplicar cambios del esquema a una base
-- existente se recrea la base (DROP DATABASE sendera;) y se vuelve a ejecutar este archivo.
-- Mantener este archivo sincronizado con las entidades JPA: Hibernate corre con ddl-auto=validate.

CREATE DATABASE IF NOT EXISTS sendera
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE sendera;

CREATE TABLE IF NOT EXISTS usuarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    apellidos VARCHAR(120) NOT NULL,
    correo VARCHAR(160) NOT NULL,
    contrasena_hash VARCHAR(100) NOT NULL,
    rol VARCHAR(20) NOT NULL DEFAULT 'ROLE_CLIENTE',
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_registro DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_usuarios_correo (correo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Registro de acciones administrativas (RF-013). No guarda contrasenas, tokens ni datos personales sensibles:
-- solo quien actuo, que hizo, sobre que recurso y cuando.
CREATE TABLE IF NOT EXISTS auditoria_admin (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    admin_id BIGINT NULL,
    accion VARCHAR(60) NOT NULL,
    recurso VARCHAR(40) NOT NULL,
    recurso_id BIGINT NULL,
    detalle VARCHAR(500) NULL,
    creada_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_auditoria_admin_creada (creada_en),
    INDEX idx_auditoria_admin_admin (admin_id),
    CONSTRAINT fk_auditoria_admin_usuario
        FOREIGN KEY (admin_id) REFERENCES usuarios (id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tokens_recuperacion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expira_en DATETIME NOT NULL,
    intentos INT NOT NULL DEFAULT 0,
    creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_tokens_recuperacion_hash (token_hash),
    INDEX idx_tokens_recuperacion_usuario (usuario_id),
    CONSTRAINT fk_tokens_recuperacion_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS destinos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    descripcion TEXT NOT NULL,
    ubicacion VARCHAR(255) NOT NULL,
    precio_base DECIMAL(12, 2) NOT NULL,
    dificultad VARCHAR(20) NOT NULL DEFAULT 'MODERADA',
    accesibilidad TINYINT(1) NOT NULL DEFAULT 0,
    CONSTRAINT chk_destinos_precio_base CHECK (precio_base >= 0),
    CONSTRAINT chk_destinos_dificultad CHECK (dificultad IN ('BAJA', 'MODERADA', 'ALTA')),
    INDEX idx_destinos_nombre (nombre),
    INDEX idx_destinos_ubicacion (ubicacion)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS destino_fotos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    destino_id BIGINT NOT NULL,
    foto_url VARCHAR(255) NOT NULL,
    UNIQUE KEY uq_destino_fotos_destino_url (destino_id, foto_url),
    CONSTRAINT fk_destino_fotos_destino
        FOREIGN KEY (destino_id) REFERENCES destinos (id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS resenas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    destino_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    comentario VARCHAR(2000) NOT NULL,
    calificacion INT NOT NULL,
    imagen LONGBLOB NULL,
    imagen_content_type VARCHAR(20) NULL,
    creada_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_resenas_calificacion CHECK (calificacion BETWEEN 1 AND 5),
    INDEX idx_resenas_destino_creada (destino_id, creada_en),
    INDEX idx_resenas_usuario_id (usuario_id),
    CONSTRAINT fk_resenas_destino
        FOREIGN KEY (destino_id) REFERENCES destinos (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_resenas_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS itinerarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    destino_id BIGINT NOT NULL,
    nombre VARCHAR(180) NOT NULL,
    descripcion TEXT NOT NULL,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    precio_base DECIMAL(12, 2) NOT NULL,
    cupos_disponibles INT NOT NULL DEFAULT 10,
    CONSTRAINT chk_itinerarios_fechas CHECK (fecha_fin >= fecha_inicio),
    CONSTRAINT chk_itinerarios_precio CHECK (precio_base >= 0),
    CONSTRAINT chk_itinerarios_cupos CHECK (cupos_disponibles >= 0),
    INDEX idx_itinerarios_destino_id (destino_id),
    INDEX idx_itinerarios_fecha_inicio (fecha_inicio),
    CONSTRAINT fk_itinerarios_destino
        FOREIGN KEY (destino_id) REFERENCES destinos (id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS reservas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    itinerario_id BIGINT NOT NULL,
    fecha_reserva DATE NOT NULL,
    monto_total DECIMAL(12, 2) NOT NULL,
    estado_reserva ENUM('PENDIENTE', 'CONFIRMADA', 'CANCELADA') NOT NULL DEFAULT 'PENDIENTE',
    CONSTRAINT chk_reservas_monto_total CHECK (monto_total >= 0),
    INDEX idx_reservas_usuario_id (usuario_id),
    INDEX idx_reservas_itinerario_id (itinerario_id),
    INDEX idx_reservas_fecha_reserva (fecha_reserva),
    INDEX idx_reservas_estado_reserva (estado_reserva),
    CONSTRAINT fk_reservas_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_reservas_itinerario
        FOREIGN KEY (itinerario_id) REFERENCES itinerarios (id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Proceso 3: itinerarios personales y catálogo de actividades (decisión del 2026-10-27, docs/04 §4.8).
-- Los paquetes ofrecidos por la agencia siguen en `itinerarios`; un viaje del usuario vive en `viajes`.
CREATE TABLE IF NOT EXISTS actividades (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    destino_id BIGINT NOT NULL,
    nombre VARCHAR(180) NOT NULL,
    descripcion TEXT NOT NULL,
    duracion_minutos INT NOT NULL,
    dificultad VARCHAR(20) NOT NULL DEFAULT 'BAJA',
    precio_por_persona DECIMAL(12, 2) NOT NULL,
    accesibilidad TINYINT(1) NOT NULL DEFAULT 0,
    CONSTRAINT chk_actividades_duracion CHECK (duracion_minutos > 0 AND duracion_minutos <= 1440),
    CONSTRAINT chk_actividades_dificultad CHECK (dificultad IN ('BAJA', 'MODERADA', 'ALTA')),
    CONSTRAINT chk_actividades_precio CHECK (precio_por_persona >= 0),
    INDEX idx_actividades_destino_id (destino_id),
    CONSTRAINT fk_actividades_destino
        FOREIGN KEY (destino_id) REFERENCES destinos (id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS viajes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_viajes_fechas CHECK (fecha_fin >= fecha_inicio),
    INDEX idx_viajes_usuario_id (usuario_id),
    CONSTRAINT fk_viajes_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS viaje_actividades (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    viaje_id BIGINT NOT NULL,
    actividad_id BIGINT NOT NULL,
    fecha DATE NOT NULL,
    hora_inicio TIME NOT NULL,
    orden INT NOT NULL,
    CONSTRAINT chk_viaje_actividades_orden CHECK (orden >= 1),
    INDEX idx_viaje_actividades_viaje (viaje_id, fecha, orden),
    INDEX idx_viaje_actividades_actividad (actividad_id),
    CONSTRAINT fk_viaje_actividades_viaje
        FOREIGN KEY (viaje_id) REFERENCES viajes (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_viaje_actividades_actividad
        FOREIGN KEY (actividad_id) REFERENCES actividades (id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
