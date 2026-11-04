-- Proceso 3: itinerarios personales y actividades. Aplicar UNA vez en las bases creadas antes de esta entrega
-- (p. ej. la de Railway) ANTES de desplegar: sin estas tablas la aplicación no arranca, porque
-- ddl-auto=validate falla con "missing table [actividades]". Las bases nuevas ya las traen en docs/schema.sql.
-- Es seguro de repetir (CREATE TABLE IF NOT EXISTS).
-- Cambia USE sendera; por la base que use tu entorno (DB_NAME). En Railway el valor por defecto es `sendera`.

USE sendera;

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
