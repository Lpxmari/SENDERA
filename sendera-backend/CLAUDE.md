# SENDERA — Instrucciones para Claude Code

## 1. Propósito
SENDERA es una aplicación web de turismo orientada principalmente a adultos mayores. Busca centralizar la consulta, comparación, organización y reserva de destinos y actividades turísticas, considerando precio, duración, dificultad y accesibilidad.

## 2. Stack definido por el proyecto
- Frontend: React + TypeScript.
- Backend: Java + Spring Boot.
- API: REST sobre HTTPS, versionada bajo `/api/v1`.
- Persistencia: MySQL mediante Spring Data JPA/Hibernate.
- Seguridad: Spring Security y autenticación basada en JWT.
- Integraciones previstas: proveedor turístico, pasarela de pagos y servicio de notificaciones/correo.

No cambies estas tecnologías sin presentar una propuesta y esperar aprobación.

## 3. Fuentes de verdad
Antes de modificar código, consulta según corresponda:
- `docs/01-contexto-del-proyecto.md`
- `docs/02-requisitos-funcionales.md`
- `docs/03-requisitos-no-funcionales.md`
- `docs/04-modelo-entidad-relacion.md`
- `docs/05-arquitectura-backend.md`
- `docs/06-seguridad-y-autorizacion.md`
- `docs/07-contrato-api.md`
- `docs/openapi.yaml` (contrato técnico compartido entre backend y frontend)
- `docs/08-estrategia-de-pruebas.md`
- `docs/09-trazabilidad-requisitos.md`
- `docs/10-plan-implementacion.md`
- `docs/convenciones-integracion.md`
- Las reglas aplicables en `.claude/rules/` y las habilidades de `.claude/skills/`.

Si dos documentos se contradicen, no elijas silenciosamente: informa la contradicción y solicita una decisión o registra una propuesta.

## 4. Regla crítica: MER pendiente
El modelo entidad-relación (MER) aún no ha sido aprobado. No inventes tablas, relaciones, cardinalidades, restricciones ni campos definitivos. Puedes proponer un modelo candidato en `docs/04-modelo-entidad-relacion.md`, claramente marcado como PROPUESTA, pero no implementes migraciones ni entidades persistentes basadas en él hasta que el equipo lo apruebe.

## 5. Arquitectura y responsabilidad
- Mantén separación por capas: `controller`, `service`, `repository`, `entity`/`model`, `dto`, `exception`, `config` e `integration` cuando aplique.
- Los controladores manejan HTTP y delegan los casos de uso al servicio.
- Los servicios contienen reglas de negocio y coordinan transacciones.
- Los repositorios encapsulan el acceso a datos.
- Los DTOs definen contratos de entrada/salida; no expongas entidades JPA directamente en la API.
- Una función debe tener una responsabilidad clara. Evita duplicación, clases excesivamente grandes y abstracciones sin necesidad.
- No mezcles lógica de negocio en React ni consultas a BD en controladores.
- No agregues dependencias nuevas sin justificar su necesidad.

## 6. Convenciones obligatorias
- Base de datos: tablas en plural y `snake_case`, por ejemplo `destinos_turisticos`; columnas en `snake_case` y nombre singular o descriptivo, por ejemplo `destino_id`, `fecha_reserva`. Todo en minúsculas.
- Java/Spring: clases en `PascalCase`; métodos y variables en `camelCase`, por ejemplo `ReservaController`, `crearReserva`, `destinoId`.
- React/TypeScript: componentes, interfaces y tipos en `PascalCase`; variables, funciones y archivos de servicios en `camelCase`, por ejemplo `TarjetaDestino.tsx`, `Destino`, `fetchDestinos`, `destinoService.ts`.
- API REST: rutas en minúsculas, plurales y `kebab-case`, con prefijo `/api/v1`, por ejemplo `/api/v1/destinos-turisticos`.
- Manejadores de eventos de UI: prefijo `handle`, por ejemplo `handleSubmit`.
- Funciones de consulta de API en frontend: `fetch...` o `get...`, con retorno tipado explícito.
- Tipos compartidos del frontend: `src/types/`.
- Sigue también `docs/convenciones-integracion.md`.

## 7. Seguridad no negociable
- No guardes contraseñas en texto plano. Usa un `PasswordEncoder` seguro de Spring Security (BCrypt como mínimo; Argon2 si el equipo lo adopta).
- No confíes en roles enviados por el frontend. El backend valida firma, expiración y claims necesarios del JWT y aplica autorización en cada endpoint.
- Verifica permisos por recurso: un usuario no debe acceder o modificar itinerarios/reservas de otra persona salvo permiso explícito.
- No registres contraseñas, JWT completos, secretos, tokens de recuperación ni datos de pago.
- Usa validación en servidor, consultas parametrizadas/JPA, HTTPS en despliegue y secretos fuera del repositorio.
- No almacenes datos completos de tarjetas en SENDERA. La pasarela externa debe procesar los datos de pago.
- Maneja CORS con orígenes permitidos, no con comodines en producción.
- La validación del frontend mejora la experiencia, pero nunca reemplaza la validación del backend.

## 8. Integración frontend-backend guiada por OpenAPI
- `docs/openapi.yaml` es el contrato técnico compartido y la referencia principal para integrar frontend y backend.
- Antes de implementar o cambiar un endpoint, acuerda y actualiza primero el contrato OpenAPI: ruta, método, parámetros, esquemas de solicitud/respuesta, autenticación, permisos y códigos HTTP.
- El backend debe implementar y documentar el contrato OpenAPI aprobado; no debe desviarse silenciosamente del esquema.
- El frontend debe consumir el contrato aprobado y usar tipos/cliente generados desde OpenAPI cuando la configuración del proyecto lo permita. No dupliques manualmente tipos que puedan generarse de forma fiable.
- Si el código y OpenAPI difieren, no adaptes un lado de manera unilateral: identifica la diferencia, corrige el contrato y sincroniza ambos lados.
- Cada cambio del contrato debe incluir pruebas y actualizar la documentación y la trazabilidad.
- No presentes rutas candidatas como definitivas hasta que se incorporen y aprueben en OpenAPI.

## 9. Errores y API
- Mantén un formato uniforme de error compatible con `ApiError` descrito en `docs/07-contrato-api.md` y `docs/openapi.yaml`.
- Usa códigos HTTP coherentes: 200, 201, 204 cuando corresponda, 400, 401, 403, 404, 409, 422 si el contrato lo adopta, y 500 para errores inesperados.
- No devuelvas stack traces, SQL, secretos ni detalles internos al cliente.
- No ocultes errores de red en frontend: presenta mensajes comprensibles y conserva el error para que la capa superior decida cómo manejarlo.
- Evita capturar excepciones para ignorarlas o devolver valores ficticios.

## 10. Accesibilidad
Diseña para adultos mayores: textos legibles, navegación clara, objetivos táctiles amplios, lenguaje sencillo, estados visibles y contraste mínimo de 4.5:1 para texto normal cuando aplique. No dependas únicamente del color para comunicar estados. Confirma los criterios exactos con los RNF aprobados.

## 11. Flujo obligatorio antes de programar
1. Lee los documentos y reglas relacionados.
2. Resume el requisito y su identificador (RF/RNF).
3. Identifica capas, DTOs, endpoints, permisos y pruebas afectados.
4. Señala dudas, decisiones pendientes y dependencias del MER.
5. Propón un plan pequeño antes de hacer cambios.

## 12. Criterios para dar una tarea por terminada
- Implementación alineada con el requisito.
- Validación de entrada en frontend cuando mejora UX y obligatoriamente en backend.
- Autorización y propiedad del recurso verificadas.
- Manejo de errores consistente.
- Pruebas relevantes ejecutadas y resultados informados.
- Documentación y trazabilidad actualizadas.
- No se afirma que una prueba pasó si no se ejecutó.
