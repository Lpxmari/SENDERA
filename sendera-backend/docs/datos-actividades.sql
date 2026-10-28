-- Catálogo de ejemplo de actividades (Proceso 3). Ejecutar DESPUÉS de docs/schema.sql y de cargar los destinos
-- (docs/datos-ejemplo.sql en desarrollo; en producción, los destinos reales).
-- Es contenido de catálogo, no contiene cuentas ni credenciales. Seguro de repetir: no duplica actividades
-- (se identifican por destino + nombre) y se omiten las de destinos que no existan.
--
-- Cambia USE sendera; por la base que use tu entorno (DB_NAME). En Railway el valor por defecto es `sendera`.

USE sendera;

INSERT INTO actividades (destino_id, nombre, descripcion, duracion_minutos, dificultad, precio_por_persona, accesibilidad)
SELECT d.id, v.nombre, v.descripcion, v.duracion_minutos, v.dificultad, v.precio_por_persona, v.accesibilidad
FROM (
    SELECT 'Hotel Boutique Casa Aurora' AS destino, 'Recorrido por el centro histórico' AS nombre,
           'Caminata guiada y tranquila por la ciudad amurallada, con pausas para descansar en las plazas.' AS descripcion,
           180 AS duracion_minutos, 'BAJA' AS dificultad, 80000.00 AS precio_por_persona, 1 AS accesibilidad
    UNION ALL SELECT 'Hotel Boutique Casa Aurora', 'Visita al Museo de Historia',
           'Recorrido por las salas del museo con guía, en un edificio con ascensor y rampas.', 120, 'BAJA', 45000.00, 1
    UNION ALL SELECT 'Hotel Boutique Casa Aurora', 'Paseo en coche de caballos',
           'Vuelta de una hora por las calles coloniales al atardecer.', 60, 'BAJA', 60000.00, 1
    UNION ALL SELECT 'Finca Hotel Los Arrayanes', 'Visita a finca cafetera',
           'Recorrido por el cafetal, proceso del café y degustación incluida.', 120, 'BAJA', 55000.00, 1
    UNION ALL SELECT 'Finca Hotel Los Arrayanes', 'Caminata corta por el Valle de Cocora',
           'Sendero suave entre palmas de cera con guía local y paradas para descansar.', 240, 'MODERADA', 90000.00, 0
    UNION ALL SELECT 'Finca Hotel Los Arrayanes', 'Taller de preparación de café',
           'Aprende a preparar café con métodos tradicionales y llévate tu propia mezcla.', 90, 'BAJA', 40000.00, 1
    UNION ALL SELECT 'Cabañas Playa Dorada', 'Paseo en lancha por la bahía',
           'Recorrido de dos horas por la costa con parada para ver el atardecer.', 120, 'BAJA', 70000.00, 1
    UNION ALL SELECT 'Cabañas Playa Dorada', 'Clase de cocina costeña',
           'Prepara un almuerzo típico con un cocinero local y comparte la mesa.', 150, 'BAJA', 65000.00, 1
    UNION ALL SELECT 'Posada Colonial El Aljibe', 'Recorrido por la plaza y el casco antiguo',
           'Caminata guiada por calles empedradas con historia de la región.', 150, 'BAJA', 50000.00, 1
    UNION ALL SELECT 'Posada Colonial El Aljibe', 'Visita a un viñedo',
           'Recorrido por las viñas y degustación de vinos de la zona.', 180, 'BAJA', 75000.00, 1
    UNION ALL SELECT 'Hotel Mirador del Lago', 'Avistamiento de aves',
           'Salida al amanecer con guía para observar aves alrededor del lago.', 180, 'MODERADA', 60000.00, 0
    UNION ALL SELECT 'Hotel Mirador del Lago', 'Paseo en bote por el lago',
           'Recorrido tranquilo por el agua, apto para toda la familia.', 60, 'BAJA', 35000.00, 1
) AS v
JOIN destinos d ON d.nombre = v.destino
WHERE NOT EXISTS (
    SELECT 1 FROM actividades a WHERE a.destino_id = d.id AND a.nombre = v.nombre
);
