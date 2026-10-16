-- Datos de ejemplo de SENDERA (solo desarrollo). Ejecutar manualmente DESPUÉS de inicializar el esquema.
-- No se ejecuta al arrancar: los UPDATE de cupos fijan los itinerarios 1 y 2 a valores de demostración.
-- Usuarios de prueba: admin@sendera.com y cliente@sendera.com, contraseña de ambos: Sendera2026
-- (no usar estas credenciales fuera de desarrollo).

USE sendera;

-- 1) Destinos
INSERT INTO destinos (id, nombre, descripcion, ubicacion, precio_base) VALUES
  (1, 'Parque Nacional Natural Tayrona', 'Senderos costeros, playas y bosque tropical con recorridos de dificultad moderada.', 'Santa Marta, Magdalena', 145000.00),
  (2, 'Valle de Cocora', 'Paisajes de palma de cera y rutas guiadas por el paisaje cultural cafetero.', 'Salento, Quindio', 85000.00),
  (3, 'Caño Cristales', 'Recorrido natural por rios y pozas de colores en la Serrania de la Macarena.', 'La Macarena, Meta', 220000.00),
  (4, 'Desierto de la Tatacoa', 'Observacion del paisaje desertico, senderos interpretativos y cielo nocturno.', 'Villavieja, Huila', 95000.00),
  (5, 'Parque Nacional Natural Los Nevados', 'Experiencia de montana entre paramos, lagunas y volcanes.', 'Manizales, Caldas', 175000.00)
ON DUPLICATE KEY UPDATE id = id;

-- 2) Fotos de los destinos
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

-- 3) Itinerarios (cupos ya descontados por las reservas activas 101 y 102)
INSERT INTO itinerarios (id, destino_id, nombre, descripcion, fecha_inicio, fecha_fin, precio_base, cupos_disponibles) VALUES
  (1, 1, 'Tayrona accesible en tres dias', 'Visita guiada con pausas, transporte local y alojamiento cercano al parque.', '2026-11-10', '2026-11-12', 435000.00, 11),
  (2, 2, 'Cocora y cultura cafetera', 'Caminata guiada de baja dificultad y visita a una finca cafetera.', '2026-11-18', '2026-11-19', 195000.00, 9),
  (3, 3, 'Expedicion a Caño Cristales', 'Recorrido guiado de naturaleza con transporte y alojamiento en La Macarena.', '2026-12-03', '2026-12-06', 890000.00, 8),
  (4, 4, 'Tatacoa y observacion astronomica', 'Senderos al atardecer y actividad de observacion del cielo.', '2027-01-15', '2027-01-16', 210000.00, 14),
  (5, 5, 'Paramo de Los Nevados', 'Ruta guiada de montana con aclimatacion y paradas de descanso.', '2027-02-10', '2027-02-12', 525000.00, 8)
ON DUPLICATE KEY UPDATE id = id;

-- Si el catálogo ya existía (por ejemplo, ejecutaste docs/schema.sql), deja los cupos coherentes
UPDATE itinerarios SET cupos_disponibles = 11 WHERE id = 1;
UPDATE itinerarios SET cupos_disponibles = 9  WHERE id = 2;

-- 4) Usuarios de prueba (hash BCrypt de "Sendera2026", generado con BCryptPasswordEncoder)
INSERT INTO usuarios (nombre, apellidos, correo, contrasena_hash, rol) VALUES
  ('Admin',   'Sendera', 'admin@sendera.com',   '$2a$10$LUCimwoN9l6uCh.e2F8LyeVHHVJt5/No6SPxo22n6YCpIMNQqFgQK', 'ROLE_ADMIN'),
  ('Cliente', 'Prueba',  'cliente@sendera.com', '$2a$10$LUCimwoN9l6uCh.e2F8LyeVHHVJt5/No6SPxo22n6YCpIMNQqFgQK', 'ROLE_CLIENTE')
ON DUPLICATE KEY UPDATE correo = correo;

-- 5) Reservas de ejemplo, propiedad del cliente de prueba
INSERT INTO reservas (id, usuario_id, itinerario_id, fecha_reserva, monto_total, estado_reserva) VALUES
  (101, (SELECT id FROM usuarios WHERE correo = 'cliente@sendera.com'), 1, '2026-10-03', 435000.00, 'PENDIENTE'),
  (102, (SELECT id FROM usuarios WHERE correo = 'cliente@sendera.com'), 2, '2026-10-03', 195000.00, 'CONFIRMADA'),
  (103, (SELECT id FROM usuarios WHERE correo = 'cliente@sendera.com'), 3, '2026-10-03', 890000.00, 'CANCELADA')
ON DUPLICATE KEY UPDATE id = id;
