# 11. Integración y despliegue continuos (CI/CD)

Los workflows viven en la **raíz del repositorio** (`.github/workflows/`), porque GitHub solo lee ese lugar, y cubren el backend y el frontend.

## Flujo

```
feature-* ──► dev ──► main        (solo main despliega)

Push o pull request a dev ──► CI: secretos + backend + frontend           (nunca despliega)
Pull request a main       ──► CI: secretos + backend + frontend           (nunca despliega)
Push/merge a main         ──► CI: secretos + backend + frontend ──► CD: desplegar ──► prueba de humo
```

El CI se ejecuta en `push` y `pull_request` hacia `main` y `dev` (y a mano con `workflow_dispatch`). El CD conserva su condición (`github.event_name == 'push' && github.ref == 'refs/heads/main'`), así que **nunca corre en `dev` ni en pull requests**.

### CI (`.github/workflows/ci.yaml`)
| Trabajo | Qué ejecuta |
|---|---|
| **0. Escaneo de secretos** (gitleaks) | Revisa **todo el historial** de git (`fetch-depth: 0`) con `gitleaks/gitleaks-action@v2` en busca de credenciales filtradas (tokens, claves de API, contraseñas). Si encuentra alguna, el trabajo falla y bloquea el CD. Ver «Escaneo de secretos». |
| **1. Backend** (`sendera-backend`, Java 21) | `./gradlew check`: compila, corre las pruebas (H2, perfil `test`, sin MySQL), genera el informe JaCoCo y exige **70 % de cobertura de líneas**. Después `bootJar` para comprobar que el jar se arma. Sube los informes como artefacto. |
| **1b. SonarQube** (opcional) | Solo si existe el secreto `SONAR_TOKEN` y el PR no viene de un fork. Usa el XML de JaCoCo. |
| **2. Frontend** (`sendera-front`, Node 22) | `npm ci` (según `package-lock.json`), `tsc --noEmit`, `oxlint`, Vitest y `vite build`. Sube `dist` como artefacto. |

Una ejecución nueva sobre la misma rama cancela la anterior en los pull requests.

### Escaneo de secretos (gitleaks)
**Propósito:** que ninguna credencial llegue o permanezca en el repositorio sin que nadie lo note (hallazgos NC-03a y OM-05 de la auditoría externa). Corre en cada push y pull request a `main` y `dev`, y el CD depende de este trabajo (`needs: [secretos, backend, frontend]`).

- Usa las reglas por defecto de gitleaks. El repositorio es de una cuenta personal, así que no requiere `GITLEAKS_LICENSE`; solo `GITHUB_TOKEN`, que GitHub entrega al workflow.
- El workflow mantiene `permissions: contents: read`. Si algún día se quiere que gitleaks comente en los pull requests, agrega `pull-requests: write` solo en el trabajo `secretos`.
- Si el trabajo falla, **trátalo primero como una filtración real**: revoca o cambia la credencial y límpiala del código. Quitarla de un commit nuevo no la borra del historial.
- Para ejecutarlo en local sobre todo el historial (con Docker):
  ```bash
  docker run --rm -v "$PWD:/repo" zricethezav/gitleaks:latest detect --source /repo --log-opts="--all" --redact -v
  ```

**Falsos positivos y `.gitleaksignore`:** si el hallazgo no es un secreto (por ejemplo, un valor de ejemplo) o es una credencial ya invalidada, agrega su *fingerprint* (línea `Fingerprint:` del informe, con la forma `commit:archivo:regla:línea`) a `.gitleaksignore` en la raíz del repositorio, **una por línea**, con un comentario `#` encima que explique por qué es seguro ignorarla y desde cuándo. Revisa cada cambio de ese archivo en el pull request: no ignores nada que siga siendo válido. Ejemplo:
```
# Contraseña de cuenta de prueba cambiada el 10/11/2026; ya no es válida.
<commit>:<archivo>:<regla>:<línea>
```

### CD (`.github/workflows/cd.yaml`)
Se invoca desde el CI (`workflow_call`) **solo** tras un push a `main` con secretos, backend y frontend en verde. Se usa `workflow_call` y no `workflow_run` para no ejecutar código de forks con acceso a secretos.

Hay un solo ambiente (`production`) y los cuatro pasos corren en orden; si uno falla, los siguientes no se ejecutan:

1. **Desplegar backend en Railway**: sube solo la carpeta `sendera-backend` con `railway up`. Entorno protegido `production`; nunca hay dos despliegues a la vez.
2. **Prueba de humo del backend**: espera `GET {APP_URL}/api/v1/actuator/health` con `"status":"UP"`, comprueba que `GET /api/v1/destinos` responde y que `GET /api/v1/admin/usuarios` sin token devuelve **401** (que la seguridad sigue cerrada).
3. **Desplegar frontend en Railway**: sube solo `sendera-front` con `railway up`. Railway compila con Railpack (`npm ci` + `npm run build`) usando las variables del servicio y sirve `dist/` con `serve` en modo SPA.
4. **Prueba de humo del frontend**: `GET {FRONTEND_URL}/` devuelve 200 con `<div id="root">` y `GET {FRONTEND_URL}/login` también devuelve 200 (fallback de la SPA para las rutas de React Router).

La configuración de Railway está en dos archivos por servicio (`sendera-backend/` y `sendera-front/`). En el backend:
- `railpack.json`: comando de arranque (`java … -jar build/libs/app.jar`). Es necesario porque el comando por defecto de Railpack busca el jar en `*/build/libs/` (proyectos multimódulo) y este proyecto es de un solo módulo, así que la app nunca arrancaba y el chequeo de salud fallaba.
- `railway.json`: constructor (Railpack), ruta del chequeo de salud y política de reinicio.

En el frontend:
- `sendera-front/railpack.json`: arranque con `serve --single` (sirve `dist/` y devuelve `index.html` para cualquier ruta, necesario para React Router). `serve` está en `dependencies` para que siga instalado en la imagen de producción.
- `sendera-front/railway.json`: constructor Railpack y chequeo de salud en `/`.

## Configuración que debe hacer el equipo (una sola vez)

**GitHub → Settings → Secrets and variables → Actions**

| Nombre | Tipo | Para qué |
|---|---|---|
| `RAILWAY_TOKEN` | Secreto | Token de proyecto de Railway |
| `RAILWAY_SERVICE_ID` | Secreto | Id del servicio del backend |
| `RAILWAY_FRONTEND_SERVICE_ID` | Secreto | Id del servicio del frontend |
| `APP_URL` | Variable | URL pública del backend, sin barra final |
| `FRONTEND_URL` | Variable | URL pública del frontend, sin barra final |
| `VITE_API_URL` | Variable (opcional) | Solo para el build de comprobación del CI. El build que se publica lo hace Railway con su propia variable (ver abajo) |
| `SONAR_TOKEN` | Secreto (opcional) | Activa SonarQube |
| `SONAR_PROJECT_KEY`, `SONAR_HOST_URL`, `SONAR_ORGANIZATION` | Variables (opcionales) | Datos de SonarQube / SonarCloud |

Crea también el entorno **production** (Settings → Environments) y, si quieres, exige aprobación manual antes de desplegar.

**Railway → variables del servicio** (nunca en el repositorio):
`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET` (mínimo 32 caracteres), `CORS_ORIGINS` (dominio real del frontend, sin comodines), `FRONTEND_URL` y, para el correo de recuperación de contraseña, `BREVO_API_KEY` y `MAIL_REMITENTE` (un remitente verificado en Brevo). Sin ellas la app arranca, pero el código no se envía y solo queda un aviso en el log. SMTP (`MAIL_HOST`, `MAIL_USERNAME`, `MAIL_PASSWORD`) es la alternativa para local; Railway suele bloquear los puertos SMTP en planes que no son Pro. Railpack detecta Java 21 por sí mismo (opcional: `RAILPACK_JDK_VERSION=21`). Para el pago con Mercado Pago (RF-014): `MERCADOPAGO_ACCESS_TOKEN` (Access Token de prueba mientras se use el sandbox; el de producción solo al salir a producción) y `MERCADOPAGO_WEBHOOK_SECRET` (clave secreta de *Tus integraciones > Webhooks*). Sin el token la app arranca y el botón de pago responde 503. En *Tus integraciones > Webhooks > Configurar notificaciones* registra la URL `{APP_URL}/api/v1/pagos/webhook` con el evento **Pagos**: los pagos de prueba solo notifican a esa URL. El frontend no necesita variables nuevas. **No actives el perfil `dev` en producción** y no crees variables con puntos o minúsculas (como `spring.profiles.active`): Railway las pasa como secretos del build y lo rompen.

**Railway → servicio del frontend** (un servicio aparte en el mismo proyecto y ambiente):
- `VITE_API_URL` = URL completa y **https** del backend con el prefijo, por ejemplo `https://tu-api.up.railway.app/api/v1`. Vite la incrusta al compilar, así que si cambia hay que volver a desplegar. Si falta, el frontend llamaría a `/api/v1` en su propio dominio y fallaría.
- Genera un dominio público (Settings → Networking). Ese dominio va en `FRONTEND_URL` de GitHub y en `CORS_ORIGINS` y `FRONTEND_URL` del **backend** (sin comodines).
- El token de proyecto (`RAILWAY_TOKEN`) sirve para ambos servicios porque están en el mismo ambiente.

**Protección de la rama:** en Settings → Branches (para `main` y, si quieres, `dev`) exige que los trabajos `0. Escaneo de secretos (gitleaks)`, `1. Backend` y `2. Frontend` pasen antes de fusionar a `main`.

## Qué cambió en el proyecto para soportarlo
- `build.gradle`: plugin JaCoCo, umbral de cobertura (`check`), `bootJar` → `build/libs/app.jar`, dependencia `spring-boot-starter-actuator`.
- `application.yaml`: `PORT`, `DB_HOST` y `DB_NAME` desde el entorno; solo se expone `health`, sin detalles.
- `SecurityConfig`: `GET /actuator/health` público; el resto de Actuator no se expone y sigue protegido.
- Pruebas: `SaludDelServicioTest`.

## Pendiente / decisiones abiertas
- **Esquema de la base de producción:** la aplicación no crea las tablas (`ddl-auto: validate`). Se carga una vez `docs/schema.sql` en la base de Railway. Los cambios de esquema se aplican a mano (no hay migraciones, decisión del equipo): **cada vez que una entidad gane una columna o tabla, hay que ejecutar el `ALTER` en la base de Railway antes de desplegar**, o la app no arrancará y el chequeo de salud fallará. Scripts necesarios en bases ya creadas: `docs/actualizar-recuperacion-por-codigo.sql` (columna `intentos`) y `docs/actualizar-viajes-actividades.sql` (tablas `actividades`, `viajes` y `viaje_actividades`; opcionalmente `docs/datos-actividades.sql` para el catálogo de ejemplo) `docs/actualizar-pagos.sql` (tabla `pagos`) y `docs/actualizar-perfil-viajero.sql` (datos del perfil, NC-01).
- **Formato (Prettier):** `npm run format:check` no se exige hasta aplicar Prettier a los 25 archivos antiguos del frontend.
- **Cobertura:** el umbral es 70 %. Medido el 2026-10-19 en `dev`: 84,9 % (141 pruebas). Antes de añadir pruebas de correo, cuentas y filtros de destinos bajó a 66-69 %, lo que hizo fallar el CI; súbelo al aumentar las pruebas.
