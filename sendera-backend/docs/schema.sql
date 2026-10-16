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

-- Datos de ejemplo del catálogo (dificultad y accesibilidad toman los valores por defecto).
-- No hay reservas de ejemplo: toda reserva necesita un propietario.
INSERT INTO destinos (id, nombre, descripcion, ubicacion, precio_base) VALUES
    (1, 'Parque Nacional Natural Tayrona', 'Senderos costeros, playas y bosque tropical con recorridos de dificultad moderada.', 'Santa Marta, Magdalena', 145000.00),
    (2, 'Valle de Cocora', 'Paisajes de palma de cera y rutas guiadas por el paisaje cultural cafetero.', 'Salento, Quindio', 85000.00),
    (3, 'Caño Cristales', 'Recorrido natural por rios y pozas de colores en la Serrania de la Macarena.', 'La Macarena, Meta', 220000.00),
    (4, 'Desierto de la Tatacoa', 'Observacion del paisaje desertico, senderos interpretativos y cielo nocturno.', 'Villavieja, Huila', 95000.00),
    (5, 'Parque Nacional Natural Los Nevados', 'Experiencia de montana entre paramos, lagunas y volcanes.', 'Manizales, Caldas', 175000.00)
ON DUPLICATE KEY UPDATE id = id;

INSERT IGNORE INTO destino_fotos (destino_id, foto_url) VALUES
    (1, 'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=1200&q=80'),
    (1, 'https://images.unsplash.com/photo-1511497584788-876760111969?auto=format&fit=crop&w=1200&q=80'),
    (2, 'https://images.unsplash.com/photo-1470770841072-f978cf4d019e?auto=format&fit=crop&w=1200&q=80'),
    (2, 'https://images.unsplash.com/photo-1501785888041-af3ef285b470?auto=format&fit=crop&w=1200&q=80'),
    (3, 'https://images.unsplash.com/photo-1469474968028-56623f02e42e?auto=format&fit=crop&w=1200&q=80'),
    (3, 'https://images.unsplash.com/photo-1472396961693-142e6e269027?auto=format&fit=crop&w=1200&q=80'),
    (4, 'https://images.unsplash.com/photo-1441974231531-c6227db76b6e?auto=format&fit=crop&w=1200&q=80'),
    (4, 'https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?auto=format&fit=crop&w=1200&q=80'),
    (5, 'https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?auto=format&fit=crop&w=1200&q=80'),
    (5, 'https://images.unsplash.com/photo-1454496522488-7a8e488e8606?auto=format&fit=crop&w=1200&q=80');

INSERT INTO itinerarios (id, destino_id, nombre, descripcion, fecha_inicio, fecha_fin, precio_base, cupos_disponibles) VALUES
    (1, 1, 'Tayrona accesible en tres dias', 'Visita guiada con pausas, transporte local y alojamiento cercano al parque.', '2026-11-10', '2026-11-12', 435000.00, 12),
    (2, 2, 'Cocora y cultura cafetera', 'Caminata guiada de baja dificultad y visita a una finca cafetera.', '2026-11-18', '2026-11-19', 195000.00, 10),
    (3, 3, 'Expedicion a Caño Cristales', 'Recorrido guiado de naturaleza con transporte y alojamiento en La Macarena.', '2026-12-03', '2026-12-06', 890000.00, 8),
    (4, 4, 'Tatacoa y observacion astronomica', 'Senderos al atardecer y actividad de observacion del cielo.', '2027-01-15', '2027-01-16', 210000.00, 14),
    (5, 5, 'Paramo de Los Nevados', 'Ruta guiada de montana con aclimatacion y paradas de descanso.', '2027-02-10', '2027-02-12', 525000.00, 8)
ON DUPLICATE KEY UPDATE id = id;
