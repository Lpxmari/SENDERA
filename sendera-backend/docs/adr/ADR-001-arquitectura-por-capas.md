# ADR-001: Arquitectura backend por capas

- **Estado:** Propuesto
- **Fecha:** Por completar
- **Decisores:** Equipo SENDERA

## Contexto
SENDERA requiere separar la API, las reglas de negocio, la persistencia y las integraciones para facilitar pruebas, seguridad y mantenimiento.

## Decisión propuesta
Organizar Spring Boot en `controller`, `service`, `repository`, `entity`, `dto`, `exception`, `config` e `integration` cuando corresponda, bajo `com.sendera`.

## Alternativas
- Lógica de negocio dentro de controladores.
- Arquitectura de microservicios desde el inicio.
- Arquitectura por módulos sin capas explícitas.

## Consecuencias
- Facilita pruebas unitarias y de integración.
- Evita acoplar HTTP a persistencia y negocio.
- Requiere mantener límites de responsabilidad y evitar clases de servicio monolíticas.
- Revisar la decisión si el tamaño del sistema justifica otra estructura.
