# 8. Estrategia de pruebas

## 8.1 Objetivo
Verificar que los requisitos funcionales y no funcionales se cumplen, con atención especial a seguridad, accesibilidad, errores de integración y reglas de itinerarios/reservas.

## 8.2 Backend
- **Unitarias:** reglas de servicios, validaciones, transiciones de estado y conflictos de horario.
- **Integración:** repositorios, transacciones, serialización, y base de datos (H2 en memoria en el perfil `test`).
- **Controladores/API:** códigos HTTP, validación de DTOs y formato uniforme de errores.
- **Seguridad:** sin token → 401; token válido sin permisos → 403; token vencido o manipulado → rechazado; acceso a recursos ajenos → denegado.
- **Integraciones:** timeout, respuesta inválida, indisponibilidad, reintentos e idempotencia según el caso.
- **Regresión:** pruebas de los flujos que se afecten al modificar módulos.

## 8.3 Frontend
- Pruebas de componentes y servicios con respuestas correctas y errores de API.
- Verificar estados de carga, vacío, error y reintento.
- Verificar que los errores tipados se muestran con mensajes claros, sin mostrar información interna.
- Verificar formularios, navegación por teclado, foco, contraste y tamaños de controles.
- Verificar que el cliente no se usa como única barrera de autorización.

## 8.4 Casos de seguridad mínimos
- No se puede cambiar el rol enviando un campo adicional en un DTO.
- No se puede consultar ni editar itinerarios/reservas de otro usuario sin permiso.
- Contraseñas y tokens no aparecen en logs.
- Los endpoints administrativos requieren autorización.
- Entrada inválida no produce errores 500 evitables.
- Consultas no permiten inyección SQL.
- Configuración CORS no abre el acceso a cualquier origen en producción.

## 8.4.1 Casos de prueba de reservas (RF-011 / RF-012)
Los casos de esta sección aún no tienen identificadores CP- oficiales; se enlazan por el nombre de la prueba.

| Caso | Qué verifica | Prueba |
|------|--------------|--------|
| Reservar el viaje correcto | El formulario envía el `itinerarioId` elegido en la lista de `GET /destinos/{id}/itinerarios`, nunca el id del destino. | `FormularioReserva.test.tsx` |
| Fechas acotadas | El input de fecha usa `min = max(hoy, fechaInicio)` y `max = fechaFin` del itinerario elegido; al cambiar de viaje se limpia una fecha fuera de rango. | `FormularioReserva.test.tsx` |
| Sin itinerarios | Mensaje claro y envío deshabilitado; también ante un error de carga. | `FormularioReserva.test.tsx` |
| Solo itinerarios reservables | El endpoint devuelve únicamente los que tienen cupos > 0 y `fechaFin` ≥ hoy, ordenados por inicio, y 404 si el destino no existe. | `ItinerarioDisponibleIntegracionTest`, `DestinoServiceImplTest`, `SeguridadApiTest` |
| Fecha de la reserva | Obligatoria, no pasada (400) y dentro del itinerario (409). | `ReservaApiTest`, `ReservaServiceImplTest` |

## 8.4.2 Casos de prueba de duración de actividades (RNF-26 / RF-008, PROPUESTA)
Se enlazan por el nombre de la prueba; el requisito RNF-26 está pendiente de aprobación del equipo.

| Caso | Qué verifica | Prueba |
|------|--------------|--------|
| Límites de los rangos | `HASTA_60_MIN` = 1–60, `DE_61_A_180_MIN` = 61–180 y `MAS_DE_180_MIN` ≥ 181, con límites inclusivos (60/61/180/181 contra H2); combinable con `destinoId`; sin filtro devuelve todas. | `ActividadDuracionIntegracionTest` |
| Traducción y validación | El servicio convierte el rango a minutos, acepta mayúsculas/minúsculas y espacios, y rechaza valores desconocidos (400) sin consultar la base. | `ActividadServiceImplTest` |
| Contrato HTTP | `GET /actividades?duracion=` responde 200 con valor válido y 400 con mensaje claro si es desconocido. | `ViajesApiTest` |
| Destinos sin duración | `duracionMin`/`duracionMax` ya no filtran `GET /destinos` (se ignoran). | `SeguridadApiTest` |
| Interfaz | Cuatro botones grandes con `aria-pressed`, duración legible (p. ej. «2 h 30 min»), mensaje sin resultados, búsqueda en el servidor y error del filtro. | `AgregarActividad.test.tsx`, `CrearItinerario.test.tsx` |

## 8.5 Pruebas del contrato OpenAPI
- Verificar que las rutas, parámetros, esquemas de solicitud/respuesta y códigos HTTP implementados coincidan con `docs/openapi.yaml`.
- Probar que los errores respeten el esquema `ApiError`.
- Si se generan tipos o cliente TypeScript desde OpenAPI, comprobar que se regeneren y compilen al cambiar el contrato.
- Considerar una validación automatizada del contrato en CI cuando se elijan las herramientas.

## 8.6 Criterios de salida
- Pruebas pertinentes ejecutadas y resultado registrado.
- Defectos críticos y altos resueltos o aceptados explícitamente.
- Cada RF/RNF implementado enlazado a evidencia en trazabilidad.
- No afirmar “todo pasa” si no se ejecutó el conjunto relevante.
