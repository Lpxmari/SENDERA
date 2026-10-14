# 5. Arquitectura backend

## 5.1 Estilo
Se propone un backend modular con arquitectura por capas y API REST. La propuesta debe mantenerse alineada con los diagramas C4 y ADR aprobados.

## 5.2 Estructura de paquetes
Paquete raíz sugerido por el manual: `com.sendera`.

```text
backend/
└── src/
    ├── main/
    │   ├── java/com/sendera/
    │   │   ├── controller/
    │   │   ├── service/
    │   │   ├── repository/
    │   │   ├── entity/
    │   │   ├── dto/
    │   │   ├── exception/
    │   │   ├── config/
    │   │   └── integration/
    │   └── resources/
    └── test/
```

Los paquetes `config` e `integration` se incluyen cuando sean necesarios. Los nombres de módulos de dominio deben definirse después de revisar el MER y los casos de uso.

## 5.3 Responsabilidad por capa
- **controller:** recibir HTTP, validar el contrato, invocar servicios y devolver códigos/respuestas. No implementar reglas de negocio ni acceder directamente a repositorios.
- **service:** coordinar casos de uso, reglas de negocio y transacciones. Mantener funciones enfocadas en una responsabilidad.
- **repository:** consultas y persistencia mediante Spring Data JPA. No exponerlo al frontend.
- **entity:** representar el modelo persistente aprobado. No crear entidades definitivas mientras el MER esté pendiente.
- **dto:** modelos de entrada/salida de API, con validaciones adecuadas. Evitar exponer entidades.
- **exception:** excepciones de dominio y manejador global, normalmente con `@RestControllerAdvice`.
- **config:** configuración de Spring Security, CORS y otras capacidades transversales.
- **integration:** adaptadores para pasarela de pagos, proveedor turístico y notificaciones.

## 5.4 Convenciones Java
- Clases e interfaces: `PascalCase`, por ejemplo `ReservaController`, `ReservaService`.
- Métodos y variables: `camelCase`, por ejemplo `crearReserva`, `obtenerReservaPorId`, `destinoId`.
- Métodos de consulta: `obtener...`, `listar...` o `get...`.
- Métodos de creación: `crear...`, `guardar...` o `create...`.
- Métodos de modificación: `actualizar...` o `update...`.
- Métodos de eliminación: `eliminar...` o `delete...`.
- Elegir español o inglés de forma consistente por módulo; no mezclar nombres arbitrariamente.
- Evitar métodos extensos; extraer funciones solo cuando mejore la claridad y la responsabilidad.

## 5.5 Flujo de una solicitud
1. Controller recibe la petición y valida el DTO de entrada.
2. Spring Security verifica autenticación y autorización.
3. Service aplica reglas de negocio y controla la transacción.
4. Repository ejecuta consultas persistentes.
5. Service devuelve un resultado o lanza una excepción de dominio.
6. El manejador global traduce el error al contrato API.
7. Controller devuelve el código HTTP y DTO correspondiente.

## 5.6 Transacciones y persistencia
- Usar transacciones en casos de uso que requieran consistencia.
- No realizar llamadas lentas a proveedores externos dentro de una transacción de BD salvo justificación.
- Evitar N+1 y cargas de relaciones innecesarias.
- Validar entradas y aplicar consultas parametrizadas.
- Definir migraciones y esquema solo tras aprobar el MER.

## 5.7 Integraciones
Usar adaptadores dedicados para aislar formatos y credenciales de los proveedores externos. Definir timeout, tratamiento de errores, reintentos limitados, idempotencia y trazabilidad según el servicio.

## 5.8 Integración con frontend mediante OpenAPI
- `docs/openapi.yaml` será la referencia compartida para rutas, métodos HTTP, parámetros, esquemas JSON, autenticación, respuestas y errores.
- Adoptar un flujo contract-first: diseñar/revisar el contrato antes de implementar o modificar endpoints.
- El backend debe mantener su implementación alineada con el contrato y publicar documentación OpenAPI accesible en desarrollo si se configura Springdoc u otra herramienta compatible.
- El frontend debe consumir los esquemas acordados y, cuando sea viable, generar tipos TypeScript y/o un cliente API desde el documento OpenAPI. Evitar mantener tipos duplicados manualmente.
- Toda modificación incompatible debe discutirse y documentarse; coordinar los cambios de backend y frontend en el mismo trabajo o en una secuencia explícita.
- Añadir verificaciones en pruebas o CI para detectar desviaciones entre el contrato y la implementación cuando la herramienta elegida lo permita.

## 5.9 Decisiones pendientes
- Versión de Java y Spring Boot.
- Maven o Gradle.
- Herramienta de migraciones.
- Convención final de nombres de estados y DTOs.
- Si los servicios serán interfaces + implementación o clases concretas; evitar interfaces ceremoniales sin necesidad.
