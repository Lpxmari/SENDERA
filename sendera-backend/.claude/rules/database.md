---
description: Convenciones de base de datos y persistencia
paths:
  - "**/src/main/java/**/*.java"
  - "**/src/main/resources/**/*"
  - "**/docs/04-modelo-entidad-relacion.md"
---

- Tablas en plural, minúsculas y `snake_case`; columnas en `snake_case`.
- Usa restricciones de integridad y claves foráneas definidas por el MER aprobado.
- Define tablas y entidades solo a partir del MER aprobado; no introduzcas migraciones.
- No se usan migraciones: `docs/schema.sql` es la referencia del esquema y `src/main/resources/schema.sql` es su copia para inicialización automática de Spring; mantenlas sincronizadas junto con las entidades.
- La inicialización SQL corre antes de Hibernate (`ddl-auto: validate`) y debe fallar si el esquema no puede crearse; no ignores errores ni uses `DROP` para forzar el arranque.
- No construyas consultas concatenando entrada del usuario.
- Evita N+1 y carga innecesaria de relaciones.
- No confundas el nombre de una columna SQL con el nombre de propiedad Java/JSON.
