-- Garantiza una sola resena por persona y destino: agrega UNIQUE (usuario_id, destino_id) a resenas.
-- Aplicar sobre una base que ya existe (p. ej. Railway); una base nueva ya lo trae en docs/schema.sql.
-- No se usan migraciones: este script se ejecuta a mano, UNA sola vez, en este orden:
--   1) Ejecutar la consulta de duplicados (paso 1). Si devuelve filas, NO continuar con el paso 3.
--   2) Resolver los duplicados (paso 2, ya revisado por una persona).
--   3) Crear la restriccion (paso 3). Si ya existe, MySQL responde un error claro y no cambia nada.
-- Haz una copia de seguridad antes de borrar datos.

USE sendera;

-- 1) Detectar duplicados: personas con mas de una resena del mismo destino.
SELECT usuario_id,
       destino_id,
       COUNT(*)                                        AS total,
       GROUP_CONCAT(id ORDER BY creada_en DESC, id DESC) AS ids_de_la_mas_reciente_a_la_mas_antigua
FROM resenas
GROUP BY usuario_id, destino_id
HAVING COUNT(*) > 1;

-- 2) Resolver duplicados (SOLO si el paso 1 devolvio filas). Esta sentencia esta comentada a proposito:
--    borra datos reales y debe habilitarla alguien despues de revisar el resultado del paso 1.
--    Conserva la resena mas reciente de cada par (usuario, destino) y elimina las anteriores.
-- DELETE r
-- FROM resenas r
-- JOIN resenas mas_nueva
--   ON mas_nueva.usuario_id = r.usuario_id
--  AND mas_nueva.destino_id = r.destino_id
--  AND (mas_nueva.creada_en > r.creada_en OR (mas_nueva.creada_en = r.creada_en AND mas_nueva.id > r.id));

-- 3) Crear la restriccion (falla si todavia hay duplicados).
ALTER TABLE resenas
    ADD CONSTRAINT uq_resenas_usuario_destino UNIQUE (usuario_id, destino_id);
