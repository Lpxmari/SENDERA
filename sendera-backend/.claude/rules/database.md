---
description: Convenciones de base de datos y persistencia
paths:
  - "**/src/main/java/**/*.java"
  - "**/src/main/resources/**/*"
  - "**/docs/04-modelo-entidad-relacion.md"
---

- Tablas en plural, minúsculas y `snake_case`; columnas en `snake_case`.
- Usa restricciones de integridad y claves foráneas definidas por el MER aprobado.
- No crees tablas, entidades ni migraciones definitivas mientras el MER esté pendiente.
- No se usan migraciones: el esquema vive en `docs/schema.sql`; actualízalo junto con las entidades y no alteres esquemas compartidos manualmente.
- No construyas consultas concatenando entrada del usuario.
- Evita N+1 y carga innecesaria de relaciones.
- No confundas el nombre de una columna SQL con el nombre de propiedad Java/JSON.
