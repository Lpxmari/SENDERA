-- Datos de ejemplo de SENDERA (solo desarrollo). Ejecutar DESPUÉS de docs/schema.sql.
-- Seguro de repetir, salvo los dos UPDATE de cupos, que siempre dejan 11 y 9.
-- Usuarios de prueba: admin@sendera.com y cliente@sendera.com, contraseña de ambos: Sendera2026
-- (no usar estas credenciales fuera de desarrollo).

USE sendera;

-- 1) Destinos
INSERT INTO destinos (id, nombre, descripcion, ubicacion, precio_base) VALUES
  (1, 'Hotel Boutique Casa Aurora', 'Antigua casona colonial restaurada dentro de la ciudad amurallada, con corredores de piedra coralina cubiertos de plantas, un lobby de arcos iluminados con luz cálida y un bar de maderas oscuras. Sus habitaciones amplias conservan techos altos y ventiladores de época. La terraza mira al mar Caribe, a pocos pasos de las plazas y los museos. Ascensor, rampas en las zonas comunes y desayuno incluido.', 'Cartagena, Bolívar', 320000.00),
  (2, 'Finca Hotel Los Arrayanes', 'Finca cafetera de dos plantas, blanca y con balcones de madera, rodeada de jardines y con una piscina que refleja la casa al atardecer. Cuenta con habitaciones con vista al cafetal, un salón con mural de la región y zonas de descanso con hamacas. Incluye desayuno campesino, caminata corta entre los cultivos de café y juegos para los nietos. Es un lugar pensado para descansar sin prisa.', 'Salento, Quindío', 240000.00),
  (3, 'Cabañas Playa Dorada', 'Cabañas de techo de palma con paredes de ladrillo y puertas entre jardines, a pocos pasos de la playa. Cada una tiene aire acondicionado, baño privado y un pequeño porche con sombra. El complejo ofrece una gran piscina con zona de sombrillas, restaurante de comida costeña y acceso directo a la arena. Terreno plano y senderos amplios, aptos para caminar con calma.', 'Santa Marta, Magdalena', 410000.00),
  (4, 'Posada Colonial El Aljibe', 'Posada de muros blancos, vigas de madera y techos de teja de barro alrededor de un patio florecido con geranios y una pequeña fuente. Tiene pocas habitaciones, todas con balcón o ventana al jardín, y una terraza con piscina que mira a las montañas. A cinco minutos de la plaza principal. Desayuno casero y atención cercana, ideal para una escapada tranquila.', 'Villa de Leyva, Boyacá', 190000.00),
  (5, 'Hotel Mirador del Lago', 'Hotel de cabañas de colores a orillas de un lago de aguas quietas, rodeado de montañas verdes y neblina suave. Tiene un muelle para sentarse a mirar el paisaje, un kiosco cubierto para comer al aire libre, piscina con pérgola y un lobby amplio con salas cómodas. Las habitaciones dan al agua. Un lugar para desconectarse y respirar aire fresco, con senderos suaves alrededor del lago.', 'Jardín, Antioquia', 280000.00)
ON DUPLICATE KEY UPDATE id = id;

-- 2) Fotos de los destinos (archivos en sendera-front/public/imagenes/alojamientos; creditos en CREDITOS.md)
INSERT IGNORE INTO destino_fotos (destino_id, foto_url) VALUES
  (1, '/imagenes/alojamientos/casa-aurora-1.jpg'),
  (1, '/imagenes/alojamientos/casa-aurora-2.jpg'),
  (1, '/imagenes/alojamientos/casa-aurora-3.jpg'),
  (1, '/imagenes/alojamientos/casa-aurora-4.jpg'),
  (1, '/imagenes/alojamientos/casa-aurora-5.jpg'),
  (2, '/imagenes/alojamientos/los-arrayanes-1.jpg'),
  (2, '/imagenes/alojamientos/los-arrayanes-2.jpg'),
  (2, '/imagenes/alojamientos/los-arrayanes-3.jpg'),
  (2, '/imagenes/alojamientos/los-arrayanes-4.jpg'),
  (3, '/imagenes/alojamientos/playa-dorada-1.jpg'),
  (3, '/imagenes/alojamientos/playa-dorada-2.jpg'),
  (3, '/imagenes/alojamientos/playa-dorada-3.jpg'),
  (3, '/imagenes/alojamientos/playa-dorada-4.jpg'),
  (4, '/imagenes/alojamientos/el-aljibe-1.jpg'),
  (4, '/imagenes/alojamientos/el-aljibe-2.jpg'),
  (4, '/imagenes/alojamientos/el-aljibe-3.jpg'),
  (4, '/imagenes/alojamientos/el-aljibe-4.jpg'),
  (5, '/imagenes/alojamientos/mirador-del-lago-1.jpg'),
  (5, '/imagenes/alojamientos/mirador-del-lago-2.jpg'),
  (5, '/imagenes/alojamientos/mirador-del-lago-3.jpg'),
  (5, '/imagenes/alojamientos/mirador-del-lago-4.jpg');

-- 2b) Dificultad y accesibilidad de cada alojamiento
UPDATE destinos SET dificultad = 'BAJA', accesibilidad = 1 WHERE id = 1;
UPDATE destinos SET dificultad = 'BAJA', accesibilidad = 0 WHERE id = 2;
UPDATE destinos SET dificultad = 'BAJA', accesibilidad = 1 WHERE id = 3;
UPDATE destinos SET dificultad = 'BAJA', accesibilidad = 0 WHERE id = 4;
UPDATE destinos SET dificultad = 'MODERADA', accesibilidad = 1 WHERE id = 5;

-- 3) Itinerarios (cupos ya descontados por las reservas activas 101 y 102)
INSERT INTO itinerarios (id, destino_id, nombre, descripcion, fecha_inicio, fecha_fin, precio_base, cupos_disponibles) VALUES
  (1, 1, 'Fin de semana colonial en Casa Aurora', 'Dos noches con desayuno, recorrido guiado por el centro histórico a paso tranquilo y cena en el bar del hotel.', '2026-11-10', '2026-11-12', 435000.00, 11),
  (2, 2, 'Descanso cafetero en Los Arrayanes', 'Dos noches con desayuno campesino, recorrido corto por el cafetal y tarde libre junto a la piscina.', '2026-11-18', '2026-11-19', 195000.00, 9),
  (3, 3, 'Tres días frente al mar en Playa Dorada', 'Tres noches en cabaña con desayuno, acceso a la piscina y a la playa, y traslado desde el aeropuerto.', '2026-12-03', '2026-12-06', 890000.00, 8),
  (4, 4, 'Escapada colonial en El Aljibe', 'Dos noches con desayuno casero, paseo por la plaza empedrada y tarde en la terraza con vista a las montañas.', '2027-01-15', '2027-01-16', 210000.00, 14),
  (5, 5, 'Tres días junto al lago', 'Tres noches con desayuno, paseo en bote por el lago y caminata guiada de baja exigencia por los alrededores.', '2027-02-10', '2027-02-12', 525000.00, 8)
ON DUPLICATE KEY UPDATE id = id;

-- Si el catálogo ya existía (por ejemplo, ejecutaste docs/schema.sql), deja los cupos coherentes
UPDATE itinerarios SET cupos_disponibles = 11 WHERE id = 1;
UPDATE itinerarios SET cupos_disponibles = 9  WHERE id = 2;

-- 4) Usuarios de prueba (hash BCrypt de "Sendera2026", generado con BCryptPasswordEncoder)
INSERT INTO usuarios (nombre, apellidos, correo, contrasena_hash, rol) VALUES
  ('Admin',   'Sendera', 'admin@sendera.com',   '$2a$10$V3A9kjYDmfrKFY20nk7f7.tNc1zCMr/Vmd2TU206ryWlH9/Ffgo6e', 'ROLE_ADMIN'),
  ('Cliente', 'Prueba',  'cliente@sendera.com', '$2a$10$V3A9kjYDmfrKFY20nk7f7.tNc1zCMr/Vmd2TU206ryWlH9/Ffgo6e', 'ROLE_CLIENTE')
ON DUPLICATE KEY UPDATE correo = correo;

-- 5) Reservas de ejemplo, propiedad del cliente de prueba
INSERT INTO reservas (id, usuario_id, itinerario_id, fecha_reserva, monto_total, estado_reserva) VALUES
  (101, (SELECT id FROM usuarios WHERE correo = 'cliente@sendera.com'), 1, '2026-10-03', 435000.00, 'PENDIENTE'),
  (102, (SELECT id FROM usuarios WHERE correo = 'cliente@sendera.com'), 2, '2026-10-03', 195000.00, 'CONFIRMADA'),
  (103, (SELECT id FROM usuarios WHERE correo = 'cliente@sendera.com'), 3, '2026-10-03', 890000.00, 'CANCELADA')
ON DUPLICATE KEY UPDATE id = id;
