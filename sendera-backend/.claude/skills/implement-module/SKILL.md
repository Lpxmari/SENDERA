---
name: implement-module
description: Implementa un módulo o caso de uso de SENDERA de forma incremental, siguiendo requisitos, arquitectura, seguridad, convenciones y pruebas.
---

# Implementar módulo SENDERA

## Procedimiento
1. Lee `CLAUDE.md`, los RF/RNF implicados, `docs/05-arquitectura-backend.md`, `docs/06-seguridad-y-autorizacion.md`, `docs/07-contrato-api.md` y `docs/convenciones-integracion.md`.
2. Comprueba si el cambio depende del MER. Si sí y el MER no está aprobado, detente antes de crear persistencia y presenta las decisiones pendientes.
3. Resume requisito, comportamiento esperado, permisos y criterios de aceptación.
4. Propón archivos a crear/modificar y pruebas.
5. Si cambia la API, actualiza y revisa primero `docs/openapi.yaml`; úsalo como contrato compartido para backend y frontend.
6. Implementa el cambio más pequeño que cumpla el caso de uso.
7. Mantén controller/service/repository/dto/entity separados.
8. En frontend, usa el contrato OpenAPI para integrar servicios; genera tipos/cliente cuando esté configurado y crea componentes accesibles.
9. Implementa errores consistentes y códigos HTTP adecuados.
10. Añade pruebas y ejecútalas.
11. Actualiza documentación y trazabilidad.

## Convenciones
- Java: clases `PascalCase`, métodos/variables `camelCase`, paquete `com.sendera`.
- React/TypeScript: componentes/tipos `PascalCase`, variables/funciones `camelCase`, manejadores `handle...`.
- BD: tablas plurales y `snake_case`; columnas `snake_case`.
- API: `/api/v1`, rutas plurales en minúsculas y `kebab-case`.

## Restricciones
- No inventes entidades/relaciones con el MER pendiente.
- No almacenes contraseñas en texto plano ni secretos en el repositorio.
- No confíes en roles del frontend.
- No devuelvas entidades JPA directamente desde controllers.
- No declares pruebas exitosas si no se ejecutaron.

## Entrega
Informa requisitos cubiertos, archivos cambiados, decisiones, riesgos, pruebas ejecutadas y pendientes.
