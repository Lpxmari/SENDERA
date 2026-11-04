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

## 8.4.0 Equivalencia con los casos de prueba oficiales (CP)
Identificadores del documento oficial «Diseño, Reutilización y Pruebas del Software» (sección 5.2). Los ids de requisitos RNF que cita ese documento son los del plan de calidad oficial, no los provisionales de `03-requisitos-no-funcionales.md`.

| CP oficial | Título | Requisitos | Casos de este documento |
|---|---|---|---|
| CP-001 | Registro de usuario con validación de datos | RF-001 | Sin caso detallado en este documento |
| CP-002 | Inicio de sesión: credenciales válidas, incorrectas y cuenta desactivada | RF-002, RF-005 | Sin caso detallado en este documento |
| CP-003 | Recuperación de contraseña con código de 6 dígitos | RF-003, RF-015 | Sin caso detallado en este documento |
| CP-004 | Control de acceso por rol a funciones administrativas | RF-005, RF-013 | Sin caso detallado en este documento (ver §8.4, casos de seguridad mínimos) |
| CP-005 | Búsqueda de destinos con filtros combinados y valores límite | RF-006, RF-008 | §8.4.2 (todos los casos) |
| CP-006 | Consulta del detalle de un destino | RF-006, RF-007 | Sin caso detallado en este documento |
| CP-007 | Crear, editar y retirar destinos (administrador) | RF-013, RF-006 | Sin caso detallado en este documento |
| CP-008 | Crear una reserva sobre un itinerario con cupos | RF-011, RF-012 | §8.4.1 (todos los casos) |
| CP-009 | Cancelar una reserva propia y proteger las ajenas | RF-012 | Sin caso detallado en este documento |
| CP-010 | Cambio de estado de reservas por el administrador | RF-012, RF-013 | Sin caso detallado en este documento |
| CP-011 | Itinerario personalizado con detección de conflictos de horario | RF-009, RF-010 | Sin caso detallado en este documento |

«Sin caso detallado» significa solo que esta estrategia no lo desarrolla en una tabla; no afirma que no existan pruebas automatizadas. Los casos de las secciones siguientes no equivalen a ningún otro CP; si aparece un caso sin equivalente oficial, se registra aquí en lugar de inventarle un id.

## 8.4.1 Casos de prueba de reservas (RF-011 / RF-012) — equivale a CP-008
Los casos de esta sección corresponden al CP-008 oficial; se enlazan por el nombre de la prueba.

| Caso | Qué verifica | Prueba |
|------|--------------|--------|
| Reservar el viaje correcto | El formulario envía el `itinerarioId` elegido en la lista de `GET /destinos/{id}/itinerarios`, nunca el id del destino. | `FormularioReserva.test.tsx` |
| Fechas acotadas | El input de fecha usa `min = max(hoy, fechaInicio)` y `max = fechaFin` del itinerario elegido; al cambiar de viaje se limpia una fecha fuera de rango. | `FormularioReserva.test.tsx` |
| Sin itinerarios | Mensaje claro y envío deshabilitado; también ante un error de carga. | `FormularioReserva.test.tsx` |
| Solo itinerarios reservables | El endpoint devuelve únicamente los que tienen cupos > 0 y `fechaFin` ≥ hoy, ordenados por inicio, y 404 si el destino no existe. | `ItinerarioDisponibleIntegracionTest`, `DestinoServiceImplTest`, `SeguridadApiTest` |
| Fecha de la reserva | Obligatoria, no pasada (400) y dentro del itinerario (409). | `ReservaApiTest`, `ReservaServiceImplTest` |

## 8.4.2 Búsqueda y filtros: destinos y duración de actividades (RNF-26 / RF-008) — equivale a CP-005

### CP-005 — Búsqueda de destinos con filtros combinados y valores límite
| Campo | Valor |
|---|---|
| Proceso | Gestión de destinos y actividades turísticas |
| Requisitos | RF-006, RF-008 · RNF-26, RNF-01, RNF-13 |
| Tipo / prioridad | Funcional / validación de datos · Alta |
| Tester / fecha del reporte | Jeffry Smith Ceballos Bejarano · 18/10/2026 |
| Estado | **Pendiente de ejecución** con la nueva implementación |

**Destinos** (`GET /destinos`): filtros de precio, dificultad y accesibilidad, solos y combinados.

| Clase | Entrada | Resultado esperado |
|---|---|---|
| Válida | `dificultad` = `BAJA`, `baja` y `ALTA` | 200; solo destinos de esa dificultad |
| Válida | `accesibilidad` = `true` y `false` | 200; solo destinos con ese valor |
| Válida | `precioMin` = 0 con `precioMax` = 500000 | 200; solo destinos dentro del rango |
| Válida | Sin filtros | 200; todos los destinos |
| Inválida | `dificultad` = `EXTREMA` | 400 con mensaje claro |
| Inválida | `precioMin` = -1 | 400 con mensaje claro |
| Inválida | `precioMin` = 600000 con `precioMax` = 500000 | 400 con mensaje claro |

**Actividades** (`GET /actividades?duracion=`): filtro por rangos hasta 60 min, 61–180 min y más de 180 min, con valores límite de 60, 61, 180 y 181 minutos; un rango inválido responde 400.

**Resultado esperado general:** solo aparecen los resultados que cumplen todos los filtros; tiempo de respuesta ≤ 2 s (RNF-01); mensajes de error claros (RNF-13).

**Cobertura automatizada actual:**
- Destinos, clases válidas e inválidas: `DestinoFiltrosCp005Test` (servicio, 14 pruebas: `baja` normalizada a `BAJA`, `ALTA`, accesibilidad `true`/`false`, `precioMin` 0 con `precioMax` 500000, mínimo igual al máximo, sin filtros, filtros combinados; y rechazo de `EXTREMA`, `precioMin` -1, `precioMax` negativo y mínimo 600000 con máximo 500000, con su mensaje y sin consultar la base).
- Destinos, contrato HTTP: `SeguridadApiTest` (los filtros llegan completos al servicio; filtro inválido → 400 con mensaje uniforme; precio o accesibilidad no válidos → 400).
- Destinos, filtrado real en base (H2): `DestinoSpecificationsIntegracionTest`.
- Actividades: rangos de duración, límites, traducción y 400 de `duracion` (tabla siguiente).
- **Sin cubrir:** la medición del tiempo de respuesta ≤ 2 s (RNF-01) no existe. El estado del CP-005 sigue siendo «pendiente de ejecución» por el equipo de pruebas con la nueva implementación.

### Casos de duración de actividades
Requisitos aprobados por el equipo. Se enlazan por el nombre de la prueba.

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
