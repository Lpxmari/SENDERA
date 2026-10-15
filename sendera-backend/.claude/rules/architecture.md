---
description: Reglas obligatorias de arquitectura para cambios del backend y frontend SENDERA
paths:
  - "**/src/main/java/**/*.java"
  - "**/sendera-front/src/**/*"
---

- Mantén separación de responsabilidades y funciones pequeñas con una única responsabilidad clara.
- En backend, los controladores gestionan HTTP; los servicios implementan casos de uso; los repositorios acceden a datos.
- No accedas a repositorios directamente desde controladores.
- No coloques reglas de negocio en componentes React.
- Usa DTOs para contratos REST; no expongas entidades JPA.
- No inventes el MER: antes de aprobarlo, no crees entidades persistentes, migraciones ni relaciones definitivas.
- `docs/openapi.yaml` es el contrato compartido frontend-backend: actualízalo antes de implementar cambios en endpoints.
- El backend y el frontend deben ajustarse al contrato OpenAPI aprobado; no mantengas contratos divergentes.
- Los cambios deben enlazarse a un RF/RNF y añadir o actualizar pruebas.
- No introduzcas microservicios o dependencias nuevas sin justificarlo y obtener aprobación.
