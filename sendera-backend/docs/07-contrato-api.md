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

## 7.3 Rutas candidatas
Son propuestas para revisión, no contratos aprobados ni garantía de que todos los recursos estén definidos por el MER.

| Método | Ruta candidata | Uso | Respuesta típica |
|---|---|---|---|
| POST | `/api/v1/usuarios` | Registrar usuario | 201 Created |
| POST | `/api/v1/auth/login` | Iniciar sesión | 200 OK |
| POST | `/api/v1/auth/logout` | Cerrar sesión, si el diseño lo requiere | 204 No Content o 200 |
| POST | `/api/v1/auth/recuperacion` | Solicitar recuperación | 200 OK con mensaje neutro |
| GET | `/api/v1/destinos-turisticos` | Listar/buscar destinos | 200 OK |
| GET | `/api/v1/destinos-turisticos/{destinoId}` | Consultar destino | 200 OK |
| GET | `/api/v1/actividades-turisticas` | Listar/filtrar actividades | 200 OK |
| GET | `/api/v1/actividades-turisticas/{actividadId}` | Consultar actividad | 200 OK |
| GET/POST | `/api/v1/itinerarios` | Listar o crear itinerarios propios | 200 / 201 |
| GET/PUT/DELETE | `/api/v1/itinerarios/{itinerarioId}` | Consultar, actualizar o eliminar | 200 / 204 |
| GET/POST | `/api/v1/reservas` | Consultar o crear reservas | 200 / 201 |
| GET/DELETE | `/api/v1/reservas/{reservaId}` | Consultar/cancelar según contrato | 200 / 204 |

Revisar los verbos y rutas finales con el equipo, las reglas de negocio y el MER aprobado.

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
