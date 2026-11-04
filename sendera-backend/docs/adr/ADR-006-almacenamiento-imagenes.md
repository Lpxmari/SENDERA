# ADR-006 — Almacenamiento de las imágenes de reseñas

**Estado: PROPUESTA.** No es una decisión aprobada. No se ha implementado ningún cambio; las imágenes siguen
guardándose en `resenas.imagen` (LONGBLOB). Requiere revisión y aprobación del equipo antes de tocar código,
esquema (`docs/schema.sql`) o contrato (`docs/openapi.yaml`).

## Contexto
Las reseñas (`POST /destinos/{destinoId}/resenas`) aceptan una imagen opcional JPEG, PNG o WebP de hasta 5 MB. Hoy
se almacena como `LONGBLOB` en MySQL y se sirve con `GET /destinos/{destinoId}/resenas/{resenaId}/imagen`.

Problemas conocidos de este enfoque:
- La base crece rápido: cada foto de hasta 5 MB engorda tablas, copias de seguridad y réplicas.
- Servir imágenes pasa por la aplicación y por la base de datos (memoria y conexiones), sin caché de red ni CDN.
- Las imágenes no se redimensionan ni se optimizan: una foto de 5 MB se descarga completa en una tarjeta.
- Escalar la aplicación a varias instancias carga la base con lecturas binarias.

## Opciones

### A. Mantener `LONGBLOB` (estado actual) con límites más estrictos
- **Ventajas:** sin dependencias nuevas ni servicios externos; una sola fuente de verdad; transaccional.
- **Desventajas:** los problemas anteriores persisten; solo se mitigan bajando el límite de tamaño o redimensionando
  en el servidor.
- **Impacto:** ninguno en la API; posible cambio de límites.

### B. Cloudinary
- **Ventajas:** almacenamiento, CDN, redimensionado y optimización automáticos mediante URL; plan gratuito para
  empezar; carga firmada desde el servidor.
- **Desventajas:** dependencia de un proveedor externo y de su plan; nueva credencial secreta (fuera del
  repositorio); hay que definir qué hacer al borrar una reseña (borrar también el archivo remoto).
- **Impacto:** la reseña guardaría una URL o un identificador público en lugar del binario; `imagenUrl` pasaría a
  ser una URL externa; sería necesaria una dependencia nueva (SDK o cliente HTTP) y su aprobación.

### C. Amazon S3 (o compatible)
- **Ventajas:** almacenamiento económico y estándar; URLs prefirmadas con expiración; control fino de acceso y de
  ciclo de vida; se puede poner un CDN delante.
- **Desventajas:** más configuración (bucket, políticas, CORS, IAM); el redimensionado no viene incluido y habría que
  resolverlo aparte; nueva credencial secreta y dependencia nueva.
- **Impacto:** igual que Cloudinary en el modelo de datos, con más trabajo de infraestructura.

## Criterios para decidir
- Costo mensual esperado y límites del plan gratuito.
- Seguridad: validación de tipo y contenido (hoy se comprueban la firma binaria y el tipo declarado), URLs firmadas,
  credenciales fuera del repositorio, y que las imágenes de una reseña eliminada se retiren.
- Accesibilidad: texto alternativo y tamaños adecuados para adultos mayores y conexiones lentas.
- Impacto sobre el MER (`docs/04`), `docs/openapi.yaml` y el frontend.
- Migración de las imágenes ya guardadas en la base.

## Pendientes antes de decidir
- Confirmar el volumen esperado de imágenes y el tamaño promedio.
- Elegir proveedor y aprobar la dependencia nueva, si se escoge B o C.
- Definir el plan de migración de los `LONGBLOB` existentes y la política de borrado.

## Decisión
Pendiente. Este documento se actualizará con la decisión del equipo.
