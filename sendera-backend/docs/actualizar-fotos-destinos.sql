-- Reemplaza las fotos de ejemplo de los destinos 1 a 5 por las imagenes locales del frontend
-- (sendera-front/public/imagenes/destinos). Ejecutar una sola vez sobre una base que ya tenga
-- los datos de ejemplo con las URLs anteriores. Las rutas son relativas: el frontend las sirve.

USE sendera;

DELETE FROM destino_fotos WHERE destino_id IN (1, 2, 3, 4, 5);

INSERT INTO destino_fotos (destino_id, foto_url) VALUES
  (1, '/imagenes/destinos/tayrona-1.jpg'),
  (1, '/imagenes/destinos/tayrona-2.jpg'),
  (2, '/imagenes/destinos/cocora-1.jpg'),
  (2, '/imagenes/destinos/cocora-2.jpg'),
  (3, '/imagenes/destinos/cano-cristales-1.jpg'),
  (3, '/imagenes/destinos/cano-cristales-2.jpg'),
  (4, '/imagenes/destinos/tatacoa-1.jpg'),
  (4, '/imagenes/destinos/tatacoa-2.jpg'),
  (5, '/imagenes/destinos/nevados-1.jpg'),
  (5, '/imagenes/destinos/nevados-2.jpg');
