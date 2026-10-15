# ADR-004 — Rutas de la API

**Estado:** aceptada (decisión del equipo, octubre 2026).

## Contexto
La documentación proponía rutas candidatas (`/api/v1/destinos-turisticos`, `/api/v1/usuarios`, `/api/v1/auth/recuperacion`), pero el backend y el frontend ya usaban `/destinos`, `/reservas` y `/auth/registro`, `/auth/recuperar-contrasena`.

## Decisión
Se mantienen las rutas implementadas. El prefijo `/api/v1` lo aporta `server.servlet.context-path`. La definición vigente está en `docs/openapi.yaml`.

## Consecuencias
- No se rompe el frontend existente.
- `docs/07-contrato-api.md` y `docs/09-trazabilidad-requisitos.md` reflejan las rutas reales.
- Los recursos nuevos deben seguir plural y `kebab-case` (por ejemplo `/api/v1/actividades-turisticas`).
- Cualquier cambio de ruta se acuerda primero en `docs/openapi.yaml`.
