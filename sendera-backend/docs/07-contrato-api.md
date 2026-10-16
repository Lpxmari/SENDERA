# 7. Contrato API REST

## 7.1 OpenAPI como contrato compartido (contract-first)
El archivo `docs/openapi.yaml` es la fuente de verdad del contrato HTTP entre frontend y backend. Este documento explica las reglas; `openapi.yaml` define de forma estructurada los endpoints y esquemas aprobados.

Flujo obligatorio:
1. Acordar el caso de uso y sus criterios de aceptación.
2. Diseñar o actualizar `docs/openapi.yaml` antes de programar el endpoint.
3. Revisar rutas, métodos, parámetros, esquemas, autenticación/autorización, códigos HTTP y errores.
4. Implementar el backend conforme al contrato.
5. Generar o sincronizar tipos TypeScript y, si se adopta, cliente API a partir del contrato.
6. Probar la implementación y el consumo desde frontend.
7. Actualizar el contrato cuando cambie una respuesta o petición; no cambiar solo el backend o solo el frontend.
8. Registrar cambios relevantes y su compatibilidad.

El contrato puede comenzar incompleto mientras los requisitos y el MER estén pendientes. Las rutas candidatas de este documento no se consideran definitivas hasta aprobarse y agregarse a `docs/openapi.yaml`.

## 7.2 Convenciones generales
- Prefijo de versión: `/api/v1`.
- Rutas en minúsculas, plural y `kebab-case`.
- JSON para solicitudes y respuestas salvo que un caso justifique otro formato.
- DTOs explícitos para entrada y salida.
- Fechas/horas en ISO 8601; definir zona horaria cuando sea una fecha-hora.
- Documentar endpoints con OpenAPI.
- Endpoints protegidos requieren el mecanismo de autenticación acordado, por ejemplo `Authorization: Bearer <token>`.

## 7.3 Rutas implementadas (decisión ADR-004)
El equipo decidió **mantener las rutas ya implementadas**. El prefijo `/api/v1` lo aporta el `context-path` del servidor; los controladores declaran rutas sin él. La definición completa y vigente está en `docs/openapi.yaml`.

| Método | Ruta | Acceso | Respuestas |
|---|---|---|---|
| POST | `/api/v1/auth/registro` | Público | 201, 400, 409 |
| POST | `/api/v1/auth/login` | Público | 200, 400, 401 |
| POST | `/api/v1/auth/recuperar-contrasena` | Público | 204, 400 |
| POST | `/api/v1/auth/cambiar-contrasena` | Público | 204, 400 |
| GET | `/api/v1/auth/me` | Autenticado | 200, 401 |
| GET | `/api/v1/destinos` (filtros: `q`, `precioMin`, `precioMax`, `duracionMin`, `duracionMax`, `dificultad`, `accesibilidad`) | Público | 200, 400 |
| GET | `/api/v1/destinos/{id}` | Público | 200, 404 |
| POST / PUT / DELETE | `/api/v1/destinos`, `/api/v1/destinos/{id}` | `ROLE_ADMIN` | 201 / 200 / 204, 400, 401, 403, 404, 409 |
| GET / POST | `/api/v1/reservas` | Autenticado | 200 / 201, 400, 401, 404, 409 |
| GET | `/api/v1/reservas/{id}` | Autenticado | 200, 401, 404 |
| PATCH | `/api/v1/reservas/{id}/cancelar` | Autenticado | 200, 401, 404, 409 |

**Propiedad de las reservas:** cada reserva pertenece al usuario del JWT. `GET /reservas` devuelve solo las propias (el administrador ve todas) y consultar o cancelar una reserva ajena responde 404, para no revelar que existe.

**Rutas aún sin implementar** (siguen siendo candidatas): actividades turísticas, itinerarios (CRUD del usuario), perfil (`PUT /auth/me`), cierre de sesión.

## 7.4 Códigos HTTP
- **200 OK:** consulta o actualización exitosa con cuerpo de respuesta.
- **201 Created:** recurso creado; incluir `Location` cuando corresponda.
- **204 No Content:** operación exitosa sin cuerpo.
- **400 Bad Request:** solicitud mal formada o validación de entrada fallida, según el contrato.
- **401 Unauthorized:** no hay autenticación válida.
- **403 Forbidden:** identidad autenticada sin permisos suficientes.
- **404 Not Found:** recurso inexistente o que no debe revelarse al usuario.
- **409 Conflict:** conflicto con estado actual, duplicado o regla de concurrencia.
- **422 Unprocessable Content:** opcional, solo si se adopta consistentemente para errores semánticos.
- **500 Internal Server Error:** fallo inesperado; no incluir stack trace ni detalles internos.

## 7.5 Formato estándar de error
Contrato base alineado con el manual de integración:

```json
{
  "timestamp": "2026-10-14T15:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "La solicitud contiene datos inválidos.",
  "path": "/api/v1/destinos-turisticos"
}
```

El campo `message` debe ser seguro y comprensible. Para validaciones por campo se puede añadir una colección `fieldErrors`, pero solo si el equipo aprueba extender el contrato. No exponer trazas, SQL, nombres internos, tokens ni secretos.

## 7.6 DTOs y tipado
- Definir DTOs de entrada y salida en backend; no serializar entidades JPA.
- En frontend, declarar interfaces y tipos en `src/types/`.
- Mantener el contrato JSON consistente entre backend y frontend.
- No asumir que los nombres de propiedades JSON deben ser iguales a los nombres `snake_case` de columnas. La base de datos usa `snake_case`; Java y TypeScript usan `camelCase`, salvo que el contrato API acuerde otra cosa.
- Validar filtros, paginación, ordenamiento y valores de enumeraciones.

## Administración (`ROLE_ADMIN`, RF-013)
Todo `/api/v1/admin/**` exige `ROLE_ADMIN` (401 sin token o con cuenta desactivada, 403 si el rol no basta).

| Método | Ruta | Descripción | Códigos |
|---|---|---|---|
| GET | `/api/v1/admin/usuarios?q=&rol=&activo=&pagina=&tamano=` | Lista paginada de usuarios | 200, 400, 401, 403 |
| GET | `/api/v1/admin/usuarios/{id}` | Detalle de un usuario | 200, 401, 403, 404 |
| PATCH | `/api/v1/admin/usuarios/{id}/rol` | Cambia el rol | 200, 400, 401, 403, 404, 409 |
| PATCH | `/api/v1/admin/usuarios/{id}/activo` | Activa o desactiva la cuenta | 200, 400, 401, 403, 404, 409 |

El login y `GET /auth/me` devuelven además `rol` en el usuario. Las acciones de usuarios y destinos
(crear, actualizar, eliminar, cambio de rol, activar/desactivar) quedan en `auditoria_admin`.
