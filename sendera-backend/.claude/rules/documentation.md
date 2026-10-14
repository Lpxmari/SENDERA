---
description: Reglas de documentación y trazabilidad
paths:
  - "backend/**/*.java"
  - "frontend/src/**/*"
  - "docs/**/*.md"
---

- Documenta intención, restricciones y reglas no obvias; evita comentarios que repitan literalmente el código.
- Usa JavaDoc en APIs públicas o lógica compleja cuando aporte contexto.
- Actualiza `docs/openapi.yaml` antes de implementar cambios de contrato y sincroniza backend, frontend, requisitos y trazabilidad.
- Registra decisiones arquitectónicas relevantes en ADRs.
- Marca las propuestas y dudas como pendientes; no las presentes como decisiones aprobadas.
