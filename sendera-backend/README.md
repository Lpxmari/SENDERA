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

`JWT_SECRET` es obligatorio también en el perfil `dev`: no existe un valor de respaldo y, sin él, la aplicación no arranca. Genera tu propia clave aleatoria de 32 o más caracteres y guárdala solo en tu `.env` local (ignorado por Git); nunca la subas al repositorio ni la compartas.

```bash
# Git Bash / Linux / macOS
openssl rand -base64 48
```

```powershell
# Windows PowerShell
$b = New-Object byte[] 48; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)
```

Copia el resultado en `.env` como `JWT_SECRET=<la clave generada>`.

El contrato HTTP vigente está en `docs/openapi.yaml`. El modelo de datos aprobado está documentado en `docs/04-modelo-entidad-relacion.md`.

### Reseñas de destinos
- Las reseñas están asociadas al destino y al usuario cuyo `uid` está firmado en el JWT; el cliente no puede enviar ni elegir otro usuario.
- `GET /api/v1/destinos/{destinoId}/resenas` es público. `POST` sobre la misma ruta requiere autenticación y recibe `multipart/form-data` con `comentario`, `calificacion` (1–5) y una `imagen` opcional.
- Solo puede reseñar un destino quien tiene una reserva `CONFIRMADA` de un itinerario de ese destino (si no, 403; también vale para administradores). Cada persona publica una única reseña por destino (si ya existe, 409); la base lo garantiza con `UNIQUE (usuario_id, destino_id)`. En una base existente, aplica `docs/actualizar-resenas-unicas.sql`.
- Se aceptan imágenes JPEG, PNG y WebP de hasta 5 MB. El archivo binario se guarda en `resenas.imagen` (LONGBLOB); el listado solo devuelve una ruta de imagen y no transfiere el BLOB hasta solicitarla.
- `GET /api/v1/destinos/{destinoId}/resenas/{resenaId}/imagen` devuelve el archivo binario para mostrarlo directamente. Los avatares no forman parte de esta funcionalidad.

### Pagos con Mercado Pago (RF-014)
- Checkout Pro por redirección: `POST /api/v1/reservas/{id}/pago` crea la preferencia (monto de la reserva, referencia `SD-` + id con 6 dígitos) y el frontend lleva al comprador a `urlPago`. Los datos de la tarjeta se escriben en Mercado Pago, nunca en SENDERA.
- Mercado Pago vuelve a `{FRONTEND_URL}/pago/resultado?reserva={id}`; esa página llama a `POST /reservas/{id}/pago/verificar`, que consulta el pago en la API de Mercado Pago. Un pago aprobado por el monto exacto en COP pasa la reserva de PENDIENTE a CONFIRMADA.
- `POST /api/v1/pagos/webhook` es público y recibe las notificaciones; si `MERCADOPAGO_WEBHOOK_SECRET` está definido exige la firma `x-signature`.
- Variables: `MERCADOPAGO_ACCESS_TOKEN` (obligatoria para pagar; sin ella el pago responde 503) y `MERCADOPAGO_WEBHOOK_SECRET` (recomendada en el despliegue). Ver `.env.example`.
- **Modo de prueba (sandbox):** usa el Access Token de *Tus integraciones > tu aplicación > Credenciales de prueba*. Paga desde una ventana de incógnito, iniciando sesión en Mercado Pago con el **comprador de prueba** de *Cuentas de prueba*, y con una tarjeta de prueba (p. ej. Mastercard `5254 1336 7440 3564`, CVV `123`, vence `11/30`) con titular `APRO` (aprobado) u `OTHE` (rechazado) y documento `123456789`.
- En local, Mercado Pago no acepta volver automáticamente a `localhost` ni puede llamar al webhook: tras pagar, regresa a SENDERA y en *Mi perfil > Consultar Historial de Viajes* pulsa **Verificar pago**; la verificación consulta el estado directamente en Mercado Pago.
- En una base existente, aplica `docs/actualizar-pagos.sql` antes de desplegar.

### Base de datos
- En el primer arranque, MySQL crea la base `sendera` si no existe y Spring ejecuta `src/main/resources/schema.sql` antes de que Hibernate valide las entidades (`ddl-auto: validate`). No se necesita ejecutar el script a mano.
- `docs/schema.sql` es la referencia para Workbench y debe mantenerse sincronizada con la copia de `src/main/resources/schema.sql`. Esta última no contiene `CREATE DATABASE` ni `USE sendera`, porque la URL JDBC selecciona la base.
- La cuenta MySQL debe tener permiso para crear la base en el primer arranque. Las tablas existentes no se migran ni se borran: `CREATE TABLE IF NOT EXISTS` conserva sus datos. Si Hibernate informa un esquema incompatible, revisa y actualiza la base de forma controlada; no la borres para solucionar el error.
- El catálogo inicial de destinos e itinerarios se inserta de forma idempotente desde el esquema. `docs/datos-ejemplo.sql` es opcional y manual: no se ejecuta al arrancar; sus actualizaciones de cupos fijan los itinerarios 1 y 2 a valores de demostración.
- Primer administrador: `UPDATE usuarios SET rol = 'ROLE_ADMIN' WHERE correo = '<correo>';`
- Las pruebas usan H2 en memoria (perfil `test`) y no ejecutan el script MySQL.
