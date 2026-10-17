-- Convierte los 5 destinos de ejemplo en alojamientos (nombre, descripcion, ubicacion, precio, fotos e itinerarios).
-- Ejecutar una sola vez sobre una base que ya tenga los datos de ejemplo anteriores (ids 1 a 5).
-- Conserva los ids, por lo que las reservas y las resenas existentes siguen apuntando al mismo alojamiento.
-- Las fotos viven en sendera-front/public/imagenes/alojamientos (creditos en CREDITOS.md).

USE sendera;

UPDATE destinos SET nombre = 'Hotel Boutique Casa Aurora', descripcion = 'Antigua casona colonial restaurada dentro de la ciudad amurallada, con corredores de piedra coralina cubiertos de plantas, un lobby de arcos iluminados con luz cálida y un bar de maderas oscuras. Sus habitaciones amplias conservan techos altos y ventiladores de época. La terraza mira al mar Caribe, a pocos pasos de las plazas y los museos. Ascensor, rampas en las zonas comunes y desayuno incluido.', ubicacion = 'Cartagena, Bolívar', precio_base = 320000.00 WHERE id = 1;
UPDATE destinos SET nombre = 'Finca Hotel Los Arrayanes', descripcion = 'Finca cafetera de dos plantas, blanca y con balcones de madera, rodeada de jardines y con una piscina que refleja la casa al atardecer. Cuenta con habitaciones con vista al cafetal, un salón con mural de la región y zonas de descanso con hamacas. Incluye desayuno campesino, caminata corta entre los cultivos de café y juegos para los nietos. Es un lugar pensado para descansar sin prisa.', ubicacion = 'Salento, Quindío', precio_base = 240000.00 WHERE id = 2;
UPDATE destinos SET nombre = 'Cabañas Playa Dorada', descripcion = 'Cabañas de techo de palma con paredes de ladrillo y puertas entre jardines, a pocos pasos de la playa. Cada una tiene aire acondicionado, baño privado y un pequeño porche con sombra. El complejo ofrece una gran piscina con zona de sombrillas, restaurante de comida costeña y acceso directo a la arena. Terreno plano y senderos amplios, aptos para caminar con calma.', ubicacion = 'Santa Marta, Magdalena', precio_base = 410000.00 WHERE id = 3;
UPDATE destinos SET nombre = 'Posada Colonial El Aljibe', descripcion = 'Posada de muros blancos, vigas de madera y techos de teja de barro alrededor de un patio florecido con geranios y una pequeña fuente. Tiene pocas habitaciones, todas con balcón o ventana al jardín, y una terraza con piscina que mira a las montañas. A cinco minutos de la plaza principal. Desayuno casero y atención cercana, ideal para una escapada tranquila.', ubicacion = 'Villa de Leyva, Boyacá', precio_base = 190000.00 WHERE id = 4;
UPDATE destinos SET nombre = 'Hotel Mirador del Lago', descripcion = 'Hotel de cabañas de colores a orillas de un lago de aguas quietas, rodeado de montañas verdes y neblina suave. Tiene un muelle para sentarse a mirar el paisaje, un kiosco cubierto para comer al aire libre, piscina con pérgola y un lobby amplio con salas cómodas. Las habitaciones dan al agua. Un lugar para desconectarse y respirar aire fresco, con senderos suaves alrededor del lago.', ubicacion = 'Jardín, Antioquia', precio_base = 280000.00 WHERE id = 5;
UPDATE destinos SET dificultad = 'BAJA', accesibilidad = 1 WHERE id = 1;
UPDATE destinos SET dificultad = 'BAJA', accesibilidad = 0 WHERE id = 2;
UPDATE destinos SET dificultad = 'BAJA', accesibilidad = 1 WHERE id = 3;
UPDATE destinos SET dificultad = 'BAJA', accesibilidad = 0 WHERE id = 4;
UPDATE destinos SET dificultad = 'MODERADA', accesibilidad = 1 WHERE id = 5;

UPDATE itinerarios SET nombre = 'Fin de semana colonial en Casa Aurora', descripcion = 'Dos noches con desayuno, recorrido guiado por el centro histórico a paso tranquilo y cena en el bar del hotel.' WHERE id = 1;
UPDATE itinerarios SET nombre = 'Descanso cafetero en Los Arrayanes', descripcion = 'Dos noches con desayuno campesino, recorrido corto por el cafetal y tarde libre junto a la piscina.' WHERE id = 2;
UPDATE itinerarios SET nombre = 'Tres días frente al mar en Playa Dorada', descripcion = 'Tres noches en cabaña con desayuno, acceso a la piscina y a la playa, y traslado desde el aeropuerto.' WHERE id = 3;
UPDATE itinerarios SET nombre = 'Escapada colonial en El Aljibe', descripcion = 'Dos noches con desayuno casero, paseo por la plaza empedrada y tarde en la terraza con vista a las montañas.' WHERE id = 4;
UPDATE itinerarios SET nombre = 'Tres días junto al lago', descripcion = 'Tres noches con desayuno, paseo en bote por el lago y caminata guiada de baja exigencia por los alrededores.' WHERE id = 5;

DELETE FROM destino_fotos WHERE destino_id IN (1, 2, 3, 4, 5);
INSERT INTO destino_fotos (destino_id, foto_url) VALUES
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
