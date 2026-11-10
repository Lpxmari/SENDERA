# 12. Respaldo y recuperación de la base de datos

Responde al hallazgo de la auditoría externa sobre la ausencia de una política de copias de seguridad (lista de verificación 5.7, 10.3 y 10.4).

## 12.1 Contexto
- La base de producción es MySQL 8.4 (base `sendera`) en Railway, en el plan Hobby.
- En el plan Hobby, Railway no permite crear ni programar backups del volumen; esa función es del plan Pro. Por eso el respaldo se hace con `mysqldump`, usando la herramienta **Data Export** de MySQL Workbench, conectada por el proxy público (TCP Proxy) del servicio MySQL.

## 12.2 Política de respaldo
| Aspecto | Regla |
|---|---|
| Frecuencia | Antes de cada paso de `dev` a `main` y, como mínimo, una vez por semana. |
| Responsable | Líder del proyecto (Mariana Ramírez Colorado). |
| Herramienta | MySQL Workbench → Server → Data Export → *Export to Self-Contained File*, con todas las tablas de `sendera`. |
| Nombre del archivo | `respaldo_sendera_AAAA-MM-DD.sql` |
| Almacenamiento | Carpeta privada del responsable. **Nunca** en el repositorio ni en una carpeta compartida, porque el archivo contiene correos y hashes de contraseñas. |
| Retención | Se conservan los 4 respaldos más recientes. |
| Verificación | Al terminar la exportación, el número de `CREATE TABLE` del archivo debe coincidir con el de tablas de producción (`SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'sendera';`), y la última línea debe ser `-- Dump completed on ...`. |

## 12.3 Procedimiento de restauración
1. Abrir una copia del archivo y comentar las líneas `CREATE DATABASE ...` y `USE \`sendera\`;`, para que la importación vaya a la base de destino elegida.
2. En MySQL Workbench, en la conexión de destino: **Server → Data Import → Import from Self-Contained File**, y en *Default Target Schema* crear o elegir la base de destino.
   - Prueba o incidente sin afectar producción: base nueva (por ejemplo, `sendera_restore`).
   - Recuperación de producción: base `sendera` en Railway, solo con la aprobación del equipo y después de sacar un respaldo del estado actual.
3. Ejecutar **Start Import** y confirmar que termina sin errores.
4. Verificar que la lista de tablas y el número de filas por tabla coinciden con el origen.
5. Si se restauró en una base nueva, apuntar el backend a ella con la variable `DB_NAME` en Railway y redesplegar.

## 12.4 Rollback del despliegue
- **Backend o frontend:** en Railway, servicio → **Deployments** → versión anterior estable → **Redeploy**.
- Si el despliegue que falla trae cambios de esquema, restaurar también la base con el respaldo sacado antes del paso a `main` (12.3).

## 12.5 Cambios de esquema
El backend arranca con `ddl-auto: validate`: si falta una tabla o una columna, la aplicación no inicia. Por eso:
- Todo PR que agregue un script `docs/actualizar-*.sql` lo indica en su descripción.
- El script se ejecuta en Railway **antes** de pasar a `main`, después del respaldo de la base.
- Ejemplo: el 10/11/2026 el CD falló porque faltaba la tabla `pagos`; se resolvió ejecutando `docs/actualizar-pagos.sql`.

## 12.6 Evidencia de la prueba de restauración (10/11/2026)
- Respaldo `respaldo_sendera_2026-11-10.sql`, generado con Data Export de Workbench desde Railway (servidor MySQL 8.4.11; cliente `mysqldump` 8.0.46).
- Restauración en una base local nueva, `sendera_restore`, sin errores.
- Las tablas y el número de filas por tabla coinciden entre Railway y `sendera_restore`.
- Capturas: exportación terminada, cabecera y final del archivo, importación terminada y comparación de conteos (anexas a la entrega).

## 12.7 Mejoras planificadas
- Automatizar el respaldo diario y cifrado con un workflow programado de GitHub Actions, o pasar al plan Pro de Railway para usar sus backups programados.