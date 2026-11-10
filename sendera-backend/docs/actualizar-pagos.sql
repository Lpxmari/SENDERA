-- RF-014: agrega la tabla de pagos (Mercado Pago Checkout Pro) a una base que ya existe (p. ej. Railway).
-- Una base nueva ya la trae en docs/schema.sql. Es seguro ejecutarlo mas de una vez: no borra ni modifica datos.
-- Debe ejecutarse ANTES de desplegar el backend con pagos: Hibernate valida el esquema (ddl-auto: validate)
-- y la aplicacion no arranca si falta la tabla.

USE sendera;

CREATE TABLE IF NOT EXISTS pagos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reserva_id BIGINT NOT NULL,
    preferencia_id VARCHAR(80) NOT NULL,
    pago_externo_id VARCHAR(40) NULL,
    estado_pago ENUM('PENDIENTE', 'APROBADO', 'RECHAZADO', 'REEMBOLSADO') NOT NULL DEFAULT 'PENDIENTE',
    monto DECIMAL(12, 2) NOT NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion DATETIME NULL,
    CONSTRAINT chk_pagos_monto CHECK (monto >= 0),
    UNIQUE KEY uq_pagos_reserva_id (reserva_id),
    INDEX idx_pagos_estado_pago (estado_pago),
    CONSTRAINT fk_pagos_reserva
        FOREIGN KEY (reserva_id) REFERENCES reservas (id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
