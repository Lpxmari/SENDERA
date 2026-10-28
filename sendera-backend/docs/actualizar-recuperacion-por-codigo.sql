-- Recuperación de contraseña por código (rama Feature-correo).
-- Aplicar UNA vez en bases creadas con la versión anterior de docs/schema.sql (p. ej. la de Railway).
-- Las bases nuevas ya incluyen la columna porque docs/schema.sql está actualizado.
-- Sin esta columna la aplicación no arranca: ddl-auto=validate falla con
-- "missing column [intentos] in table [tokens_recuperacion]".

-- Cambia USE sendera; por la base que use tu entorno (DB_NAME). En Railway el valor por defecto es `sendera`.
USE sendera;

ALTER TABLE tokens_recuperacion ADD COLUMN intentos INT NOT NULL DEFAULT 0;

-- Los tokens pendientes del formato anterior (enlace con hash SHA-256) ya no son válidos.
DELETE FROM tokens_recuperacion WHERE id > 0;
