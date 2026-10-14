# 8. Estrategia de pruebas

## 8.1 Objetivo
Verificar que los requisitos funcionales y no funcionales se cumplen, con atención especial a seguridad, accesibilidad, errores de integración y reglas de itinerarios/reservas.

## 8.2 Backend
- **Unitarias:** reglas de servicios, validaciones, transiciones de estado y conflictos de horario.
- **Integración:** repositorios, transacciones, serialización, base de datos y migraciones una vez aprobado el MER.
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
