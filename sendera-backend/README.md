# SENDERA

Aplicación web turística enfocada en adultos mayores.

## Tecnologías
- Frontend: React + TypeScript.
- Backend: Java + Spring Boot.
- Persistencia: MySQL con Spring Data JPA/Hibernate.
- API: REST versionada `/api/v1`.
- Seguridad prevista: Spring Security + JWT.

## Estructura inicial sugerida
```text
.
├── CLAUDE.md
├── docs/
│   ├── adr/
│   ├── 01-contexto-del-proyecto.md
│   ├── 02-requisitos-funcionales.md
│   ├── 03-requisitos-no-funcionales.md
│   ├── 04-modelo-entidad-relacion.md
│   ├── 05-arquitectura-backend.md
│   ├── 06-seguridad-y-autorizacion.md
│   ├── 07-contrato-api.md
│   ├── openapi.yaml
│   ├── 08-estrategia-de-pruebas.md
│   ├── 09-trazabilidad-requisitos.md
│   ├── 10-plan-implementacion.md
│   └── convenciones-integracion.md
├── .claude/
│   ├── rules/
│   └── skills/
├── frontend/
└── backend/
```

## Antes de desarrollar
1. Lee `CLAUDE.md`.
2. Revisa el contexto, requisitos y convenciones en `docs/`.
3. El MER está pendiente: no crees el esquema definitivo ni entidades persistentes hasta su aprobación.
4. Usa `docs/openapi.yaml` como contrato compartido para integrar frontend y backend. Los cambios de API deben actualizar el contrato y ambos lados.
5. Configura variables de entorno locales a partir de `.env.example` (si aplica); nunca subas secretos.

## Configuración local
Copia `.env.example`, define `DB_PASSWORD` y `JWT_SECRET` (mínimo 32 caracteres) en tu entorno y arranca con el perfil de desarrollo:

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

El contrato HTTP vigente está en `docs/openapi.yaml`. El modelo de datos aún no está aprobado (ver `docs/04-modelo-entidad-relacion.md`).

### Base de datos
- Crear la base: ejecutar `docs/schema.sql` (referencia vigente del esquema). El proyecto no usa migraciones: si el esquema cambia, se recrea la base (`DROP DATABASE sendera;`, que borra sus datos) y se ejecuta de nuevo el script.
- Primer administrador: `UPDATE usuarios SET rol = 'ROLE_ADMIN' WHERE correo = '<correo>';`
- Las pruebas usan H2 en memoria (perfil `test`) y no necesitan MySQL.
