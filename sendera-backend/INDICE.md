# Paquete documental SENDERA para Claude Code

Este paquete reúne el contexto del proyecto, requisitos iniciales, convenciones de integración, arquitectura, seguridad, API, pruebas, trazabilidad, plan, ADRs y reglas/skills para Claude Code.

## Integración frontend-backend
- `docs/openapi.yaml` es el contrato compartido: actualizarlo antes de implementar cambios de API y mantener backend y frontend sincronizados.

## Importante antes de usar
- Los requisitos y rutas marcados como candidatos deben revisarse con el equipo.
- El MER sigue pendiente: no generar entidades ni migraciones definitivas hasta aprobarlo.
- Completar `docs/03-requisitos-no-funcionales.md` con todos los RNF del plan de calidad oficial, conservando sus IDs y redacción.
- Copiar estos archivos a la raíz del repositorio SENDERA respetando sus rutas.

## Archivos
- `.claude/rules/architecture.md`
- `.claude/rules/database.md`
- `.claude/rules/documentation.md`
- `.claude/rules/error-handling.md`
- `.claude/rules/frontend-typescript.md`
- `.claude/rules/java-and-spring.md`
- `.claude/rules/security.md`
- `.claude/rules/testing.md`
- `.claude/skills/implement-module/SKILL.md`
- `.claude/skills/review-backend/SKILL.md`
- `.claude/skills/security-audit/SKILL.md`
- `.env.example`
- `.gitignore`
- `CLAUDE.md`
- `README.md`
- `docs/01-contexto-del-proyecto.md`
- `docs/02-requisitos-funcionales.md`
- `docs/03-requisitos-no-funcionales.md`
- `docs/04-modelo-entidad-relacion.md`
- `docs/05-arquitectura-backend.md`
- `docs/06-seguridad-y-autorizacion.md`
- `docs/07-contrato-api.md`
- `docs/openapi.yaml` (contrato compartido frontend-backend)
- `docs/08-estrategia-de-pruebas.md`
- `docs/09-trazabilidad-requisitos.md`
- `docs/10-plan-implementacion.md`
- `docs/11-ci-cd.md`
- `docs/adr/ADR-001-arquitectura-por-capas.md`
- `docs/adr/ADR-002-seguridad-jwt.md`
- `docs/adr/ADR-003-persistencia-relacional.md`
- `docs/convenciones-integracion.md`
