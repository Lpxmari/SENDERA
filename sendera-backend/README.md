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
3. El MER está aprobado; refleja los cambios de persistencia en `docs/schema.sql`, su copia de inicialización y las entidades.
4. Usa `docs/openapi.yaml` como contrato compartido para integrar frontend y backend. Los cambios de API deben actualizar el contrato y ambos lados.
5. Configura variables de entorno locales a partir de `.env.example` (si aplica); nunca subas secretos.

## Configuración local
Copia `.env.example` como `.env`, define `DB_PASSWORD` y `JWT_SECRET` (mínimo 32 caracteres) y arranca con el perfil de desarrollo. `DB_PORT` es opcional y usa 3306 por defecto:

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

En Windows PowerShell usa `.\gradlew.bat bootRun --args='--spring.profiles.active=dev'`.

El contrato HTTP vigente está en `docs/openapi.yaml`. El modelo de datos aprobado está documentado en `docs/04-modelo-entidad-relacion.md`.

### Reseñas de destinos
- Las reseñas están asociadas al destino y al usuario cuyo `uid` está firmado en el JWT; el cliente no puede enviar ni elegir otro usuario.
- `GET /api/v1/destinos/{destinoId}/resenas` es público. `POST` sobre la misma ruta requiere autenticación y recibe `multipart/form-data` con `comentario`, `calificacion` (1–5) y una `imagen` opcional.
- Se aceptan imágenes JPEG, PNG y WebP de hasta 5 MB. El archivo binario se guarda en `resenas.imagen` (LONGBLOB); el listado solo devuelve una ruta de imagen y no transfiere el BLOB hasta solicitarla.
- `GET /api/v1/destinos/{destinoId}/resenas/{resenaId}/imagen` devuelve el archivo binario para mostrarlo directamente. Los avatares no forman parte de esta funcionalidad.

### Base de datos
- En el primer arranque, MySQL crea la base `sendera` si no existe y Spring ejecuta `src/main/resources/schema.sql` antes de que Hibernate valide las entidades (`ddl-auto: validate`). No se necesita ejecutar el script a mano.
- `docs/schema.sql` es la referencia para Workbench y debe mantenerse sincronizada con la copia de `src/main/resources/schema.sql`. Esta última no contiene `CREATE DATABASE` ni `USE sendera`, porque la URL JDBC selecciona la base.
- La cuenta MySQL debe tener permiso para crear la base en el primer arranque. Las tablas existentes no se migran ni se borran: `CREATE TABLE IF NOT EXISTS` conserva sus datos. Si Hibernate informa un esquema incompatible, revisa y actualiza la base de forma controlada; no la borres para solucionar el error.
- El catálogo inicial de destinos e itinerarios se inserta de forma idempotente desde el esquema. `docs/datos-ejemplo.sql` es opcional y manual: no se ejecuta al arrancar; sus actualizaciones de cupos fijan los itinerarios 1 y 2 a valores de demostración.
- Primer administrador: `UPDATE usuarios SET rol = 'ROLE_ADMIN' WHERE correo = '<correo>';`
- Las pruebas usan H2 en memoria (perfil `test`) y no ejecutan el script MySQL.
