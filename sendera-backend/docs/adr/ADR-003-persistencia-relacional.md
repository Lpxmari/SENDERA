# ADR-003: MySQL y Spring Data JPA

- **Estado:** Propuesto
- **Fecha:** Por completar
- **Decisores:** Equipo SENDERA

## Contexto
SENDERA manejará información relacionada de usuarios, destinos, actividades, itinerarios y reservas, y requiere integridad transaccional.

## Decisión propuesta
Usar MySQL como base de datos relacional y Spring Data JPA/Hibernate para encapsular la persistencia, sujeto a confirmación del equipo.

## Alternativas
- PostgreSQL.
- Base de datos NoSQL.
- Consultas SQL ejecutadas directamente desde controladores.

## Consecuencias
- Se requiere aprobar el MER antes de implementar entidades y migraciones.
- Las tablas serán plurales y `snake_case`; las columnas estarán en `snake_case`.
- Se deben definir índices, restricciones, transacciones y política de migraciones.
- No se decide ningún esquema concreto con este ADR.
