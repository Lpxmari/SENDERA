-- NC-01: datos del perfil del viajero (identificacion, fecha de nacimiento, pais, destinos favoritos e intereses).
-- Aplicar en bases creadas con la version anterior de docs/schema.sql (p. ej. la de Railway); las bases nuevas
-- ya los traen. Ejecutar ANTES de desplegar el backend: con ddl-auto=validate la aplicacion no arranca si faltan.
-- Es seguro ejecutarlo mas de una vez: solo agrega lo que falta y no borra ni modifica datos.

-- Cambia USE sendera; por la base que use tu entorno (DB_NAME). En Railway el valor por defecto es `sendera`.
USE sendera;

-- MySQL 8.0 no admite ADD COLUMN IF NOT EXISTS: cada columna se agrega solo si no existe.
SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE() AND table_name = 'usuarios' AND column_name = 'numero_identificacion') = 0,
              'ALTER TABLE usuarios ADD COLUMN numero_identificacion VARCHAR(15) NULL',
              'SELECT ''numero_identificacion ya existe''');
PREPARE paso FROM @sql; EXECUTE paso; DEALLOCATE PREPARE paso;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE() AND table_name = 'usuarios' AND column_name = 'fecha_nacimiento') = 0,
              'ALTER TABLE usuarios ADD COLUMN fecha_nacimiento DATE NULL',
              'SELECT ''fecha_nacimiento ya existe''');
PREPARE paso FROM @sql; EXECUTE paso; DEALLOCATE PREPARE paso;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE() AND table_name = 'usuarios' AND column_name = 'pais_residencia') = 0,
              'ALTER TABLE usuarios ADD COLUMN pais_residencia VARCHAR(80) NULL',
              'SELECT ''pais_residencia ya existe''');
PREPARE paso FROM @sql; EXECUTE paso; DEALLOCATE PREPARE paso;

CREATE TABLE IF NOT EXISTS usuario_destinos_favoritos (
    usuario_id BIGINT NOT NULL,
    orden INT NOT NULL,
    destino VARCHAR(80) NOT NULL,
    PRIMARY KEY (usuario_id, orden),
    CONSTRAINT fk_usuario_destinos_favoritos_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS usuario_intereses (
    usuario_id BIGINT NOT NULL,
    orden INT NOT NULL,
    interes VARCHAR(60) NOT NULL,
    PRIMARY KEY (usuario_id, orden),
    CONSTRAINT fk_usuario_intereses_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
