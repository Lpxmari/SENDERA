# ADR-005 — Gestión administrativa (RF-013)

**Estado:** aprobada por el equipo el 2026-10-16 (alcance y cambios de esquema). Implementación por etapas.

## Contexto
RF-013 exige que el administrador gestione usuarios, destinos y reservas con auditoría. Hasta ahora solo existía
la escritura de destinos restringida a `ROLE_ADMIN`.

## Decisiones
- **Rutas:** las operaciones exclusivas del administrador viven bajo `/api/v1/admin/**` y una sola regla de
  `SecurityConfig` exige `ROLE_ADMIN` para todo ese prefijo. Los destinos mantienen sus rutas (ADR-004).
- **Usuarios:** no se eliminan; se **desactivan** (`usuarios.activo`) para conservar reservas y auditoría.
  Una cuenta desactivada no puede iniciar sesión y sus tokens dejan de valer en la siguiente petición
  (verificación por petición, `CuentaActivaVerificador`). El login devuelve el mismo error genérico.
- **Salvaguardas:** un administrador no puede cambiar su propio rol ni desactivarse, y siempre debe quedar
  al menos un administrador activo.
- **Auditoría:** tabla `auditoria_admin` (quién, acción, recurso, id del recurso, detalle, fecha). Se escribe
  en la misma transacción de la operación y no contiene contraseñas, tokens ni datos personales.
- **Rol en el cliente:** `UsuarioDto` incluye `rol` para que el frontend muestre u oculte el menú de
  administración. Es solo presentación: el backend autoriza siempre con el rol firmado en el JWT.
- **Primer administrador:** sigue promoviéndose con SQL (docs/04 §4.5).

## Etapas
1. Base + usuarios + destinos + registro de auditoría.
2. Reservas (listar todas, confirmar/cancelar) y moderación de reseñas.
3. Panel con resumen y pantalla de consulta de la auditoría.

## Consecuencias
- Cambio de esquema sin migraciones: en una base existente hay que ejecutar
  `ALTER TABLE usuarios ADD COLUMN activo BOOLEAN NOT NULL DEFAULT TRUE AFTER rol;` y crear `auditoria_admin`
  (se crea sola al ejecutar `docs/schema.sql`, que usa `CREATE TABLE IF NOT EXISTS`).

## Etapa 2 (implementada 2026-10-16)
- **Reservas:** `GET /admin/reservas` (filtros: estado, texto, rango de fechas, paginación) y
  `PATCH /admin/reservas/{id}/estado`. Transiciones: PENDIENTE → CONFIRMADA, PENDIENTE → CANCELADA y
  CONFIRMADA → CANCELADA; CANCELADA es final. Cancelar devuelve el cupo al itinerario con bloqueo
  pesimista, igual que al reservar. Auditoría `RESERVA_CONFIRMAR` / `RESERVA_CANCELAR`.
- **Reseñas:** `GET /admin/resenas` (destino, calificación máxima, texto) y `DELETE /admin/resenas/{id}`.
  El listado usa una proyección que no carga la imagen (LONGBLOB). La auditoría `RESENA_ELIMINAR` guarda solo
  identificadores, nunca el texto de la reseña.
