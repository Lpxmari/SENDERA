---
description: Convenciones Java y Spring Boot
paths:
  - "backend/**/*.java"
---

- Usa el paquete raíz `com.sendera`.
- Clases, interfaces y enums en `PascalCase`; métodos y variables en `camelCase`.
- Sigue las capas definidas en `docs/05-arquitectura-backend.md`.
- Usa DTOs para entradas y salidas, Bean Validation y servicios para reglas de negocio.
- Los métodos públicos deben tener nombres claros y tipos de retorno explícitos.
- Usa transacciones donde la operación requiera consistencia.
- Evita capturar excepciones para ignorarlas o devolver valores ficticios.
- Mantén coherencia de idioma en nombres del código.
