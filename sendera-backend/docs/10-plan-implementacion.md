# 10. Plan de implementación

El orden puede ajustarse en equipo, pero no se deben saltar decisiones que bloqueen la persistencia o seguridad.

## Fase 1 — Preparación del repositorio
- Crear estructura `frontend/` y `backend/`.
- Crear `CLAUDE.md`, reglas, documentación y `.gitignore`.
- Configurar herramientas de formato, lint y pruebas.
- Añadir `.env.example` sin secretos reales.

**Salida:** repositorio reproducible y reglas visibles para Claude Code.

## Fase 2 — Cierre de requisitos
- Revisar RF y RNF oficiales.
- Aclarar alcance de roles, acompañantes, pagos y proveedores.
- Especificar criterios de aceptación y prioridades.

**Salida:** requisitos aprobados y trazables.

## Fase 3 — Diseño del MER
- Diseñar entidades, relaciones, cardinalidades, estados e integridad.
- Crear diccionario de datos.
- Revisar con el equipo y registrar aprobación.

**Bloqueo:** no implementar entidades/migraciones definitivas antes de esta salida.

## Fase 4 — Fundamentos backend y frontend
- Configurar Spring Boot, validación, manejo global de errores y OpenAPI.
- Configurar React + TypeScript, estructura `components/`, `pages/`, `services/`, `types/`, `utils/`.
- Implementar contratos de error y convenciones compartidas.
- Configurar base de datos y migraciones aprobadas.

## Fase 5 — Seguridad y usuarios
- Implementar registro, autenticación, recuperación y perfil.
- Configurar Spring Security/JWT.
- Definir matriz de permisos y pruebas de propiedad de recursos.
- Revisar CORS, secretos, logs y limitación de intentos.

## Fase 6 — Catálogo turístico
- Implementar consulta de destinos/actividades y filtros.
- Definir la fuente de datos: administrador, proveedor o integración.
- Manejar paginación, errores externos y rendimiento.

## Fase 7 — Itinerarios
- Implementar creación, consulta y edición.
- Aplicar validación de horarios y disponibilidad según reglas aprobadas.
- Validar propiedad y permisos.

## Fase 8 — Reservas, pagos y notificaciones
- Implementar estados y políticas de reserva.
- Integrar pasarela solo si forma parte del alcance aprobado.
- Añadir notificaciones mediante adaptador dedicado.
- Probar fallos, idempotencia y consistencia.

## Fase 9 — Calidad y entrega
- Ejecutar pruebas unitarias, integración, seguridad, accesibilidad y rendimiento.
- Revisar trazabilidad.
- Actualizar documentación, ADRs y manual de ejecución.
- Preparar despliegue y variables de entorno.

## Regla para cada tarea
Antes de codificar, Claude Code debe indicar requisitos afectados, archivos previstos, riesgos, dudas y pruebas. Al terminar, debe resumir archivos cambiados, decisiones, comandos ejecutados y resultados reales.
