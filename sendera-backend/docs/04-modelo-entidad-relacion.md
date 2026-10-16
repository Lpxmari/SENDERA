# 4. Modelo entidad-relación (MER)

**Estado: APROBADO (confirmación recibida el 2026-10-14). Ver §4.5 y §4.6.** Registrar la aprobación formal del equipo en el historial del proyecto.

El MER todavía no está disponible. Claude Code no debe crear entidades JPA, tablas, migraciones, claves foráneas ni cardinalidades definitivas basándose solo en nombres candidatos.

## 4.1 Dominios que deberán analizarse
La documentación menciona estos conceptos de negocio, pero no confirma todavía cómo deben representarse en tablas:
- usuarios y roles;
- destinos turísticos;
- actividades turísticas;
- itinerarios y actividades asociadas;
- reservas y sus estados;
- disponibilidad;
- pagos o referencias de pago, si aplica;
- notificaciones, si se requiere persistirlas.

Esta lista es un inventario de conceptos, no un esquema aprobado.

## 4.2 Preguntas por resolver
- ¿Un usuario puede tener varios roles?
- ¿Un acompañante tendrá relación explícita con uno o más adultos mayores? ¿Qué consentimiento y permisos requiere?
- ¿Un itinerario pertenece a un usuario, puede compartirse o puede tener varios participantes?
- ¿Cómo se representa el orden, fecha y hora de cada actividad?
- ¿Una reserva se realiza por itinerario, actividad o proveedor?
- ¿Cómo se representa la disponibilidad que proviene de un tercero?
- ¿Cuáles son los estados válidos de itinerario, reserva y pago?
- ¿Se requiere borrado lógico? ¿Qué datos se conservan por auditoría?
- ¿Qué claves únicas, restricciones e índices exige el negocio?

## 4.3 Reglas para implementar una vez aprobado
- Tablas en plural y `snake_case`, todo en minúsculas.
- Columnas en `snake_case`; nombres singulares o descriptivos, por ejemplo `destino_id`, `fecha_reserva`.
- Definir PK, FK, cardinalidades, nulabilidad, restricciones, índices y política de borrado.
- No se usan migraciones: `docs/schema.sql` es la referencia del esquema y `src/main/resources/schema.sql` es la copia que Spring ejecuta al arrancar; ambos se mantienen sincronizados con las entidades.
- No serializar entidades JPA directamente en respuestas REST.
- Documentar cambios de esquema y su impacto en DTOs, servicios, frontend y pruebas.

## 4.4 Evidencia requerida para marcarlo como aprobado
- Diagrama MER revisado por el equipo.
- Diccionario de datos.
- Restricciones y reglas de integridad.
- Estados y transiciones para itinerarios/reservas/pagos.
- Aprobación explícita registrada en el historial del proyecto.


---

# 4.5 MER aprobado (antes propuesta)

> **Estado: APROBADO.** Los cambios de §4.5.3 se implementaron en la Fase C; ver §4.6 para lo que quedó aplicado y lo que sigue pendiente.

## 4.5.1 Situación actual
Existen en el código y en la BD local estas tablas, creadas antes de aprobar el MER: `usuarios`, `tokens_recuperacion`, `destinos`, `destino_fotos`, `itinerarios`, `reservas`. No hay tabla de actividades ni de pagos.

## 4.5.2 Diagrama candidato
```text
usuarios 1───N tokens_recuperacion
usuarios 1───N reservas            (NUEVA: reservas.usuario_id)
usuarios 1───N resenas             (NUEVA: resenas.usuario_id)
destinos 1───N destino_fotos
destinos 1───N resenas             (NUEVA: resenas.destino_id)
destinos 1───N itinerarios
itinerarios 1───N reservas
```

## 4.5.3 Cambios propuestos sobre lo implementado
| Cambio | Motivo | Impacto |
|---|---|---|
| `usuarios.rol` VARCHAR(20) NOT NULL DEFAULT 'ROLE_CLIENTE' | La entidad ya lo usa y el JWT lo firma; falta en los scripts SQL | Cambio de esquema; ninguna entidad nueva |
| `reservas.usuario_id` BIGINT NOT NULL, FK a `usuarios(id)`, índice | Autorización por propietario (RF-005, RF-010, RF-012) | Esquema, `Reserva`, `ReservaService`, pruebas |
| Separar estado de reserva y estado de pago: `reservas.estado_reserva` y, si el pago es parte del alcance, `estado_pago` | `estado_pago` hoy guarda el estado de la reserva | Migración, DTO, frontend |
| `destinos.dificultad` como ENUM/CHECK (`BAJA`,`MODERADA`,`ALTA`) | Hoy es texto libre | Esquema |
| Eliminar `destinos.accesibilidad` booleano o ampliarlo (tabla de características) | Decidir si basta un booleano para adultos mayores | Por decidir |
| Unificar los scripts SQL | Los 3 scripts SQL estaban desincronizados | Un único `docs/schema.sql` |

## 4.5.4 Diccionario de datos (resumen)
| Tabla | Columna | Tipo | Reglas |
|---|---|---|---|
| usuarios | id, nombre, apellidos, correo, contrasena_hash, rol, fecha_registro | BIGINT, VARCHAR, DATETIME | `correo` único; hash BCrypt (≤100) |
| tokens_recuperacion | usuario_id, token_hash, expira_en, creado_en | BIGINT, CHAR(64), DATETIME | hash SHA-256 único; borrado en cascada |
| destinos | nombre, descripcion, ubicacion, precio_base, dificultad, accesibilidad | VARCHAR, TEXT, DECIMAL(12,2), TINYINT | `precio_base >= 0` |
| destino_fotos | destino_id, foto_url | BIGINT, VARCHAR(255) | único por (destino, url) |
| itinerarios | destino_id, nombre, descripcion, fecha_inicio, fecha_fin, precio_base, cupos_disponibles | DATE, DECIMAL, INT | `fecha_fin >= fecha_inicio`; `cupos >= 0` |
| reservas | itinerario_id, **usuario_id (propuesto)**, fecha_reserva, monto_total, estado | DATE, DECIMAL, ENUM | monto lo calcula el servidor |
| resenas | destino_id, usuario_id, comentario, calificacion, imagen, imagen_content_type, creada_en | BIGINT, VARCHAR, TINYINT, LONGBLOB, DATETIME | calificación 1–5; imagen opcional |

## 4.5.5 Estados y transiciones propuestos
`PENDIENTE → CONFIRMADA → CANCELADA` y `PENDIENTE → CANCELADA`. Una reserva cancelada no vuelve a otro estado. Cancelar devuelve el cupo al itinerario.

## 4.5.6 Política de borrado propuesta
- `destinos`: no se elimina si tiene itinerarios (RESTRICT, ya implementado en el servicio).
- `itinerarios` con reservas: RESTRICT.
- `usuarios`: sin borrado físico mientras tengan reservas; evaluar borrado lógico.

## 4.5.7 Preguntas abiertas para el equipo
Las de §4.2, además de: ¿una reserva descuenta un cupo por persona o por reserva? ¿Se necesita número de participantes? ¿Existen actividades como entidad propia o el itinerario las reemplaza?


# 4.6 Implementación del MER aprobado

| Cambio de §4.5.3 | Estado |
|---|---|
| `usuarios.rol` | Aplicado (entidad y `docs/schema.sql`) |
| `reservas.usuario_id` NOT NULL, FK, índice | Aplicado; el propietario sale del claim `uid` del JWT |
| `estado_pago` → `estado_reserva` | Aplicado (columna, entidad, DTO `estadoReserva`, contrato y frontend). `estado_pago` se añadirá si el pago entra en el alcance |
| `dificultad` restringida | Aplicado como `CHECK` en la BD y `@Pattern` en el DTO; la entidad sigue usando `String` |
| `accesibilidad` | Se mantiene como booleano (decisión pendiente de ampliarla) |
| Migraciones | **No se usan** (decisión del equipo). Spring crea las tablas faltantes desde `src/main/resources/schema.sql`; `docs/schema.sql` es la referencia para ejecución manual |

**Primer administrador:** no existe un endpoint para crearlo. Se promueve una cuenta con `UPDATE usuarios SET rol = 'ROLE_ADMIN' WHERE correo = '...'`.

**Referencia del esquema:** `docs/schema.sql` reemplaza a los scripts anteriores desincronizados. La copia `src/main/resources/schema.sql` se ejecuta al arrancar y debe mantenerse sincronizada. Una base creada con el esquema anterior no se migra automáticamente; alinear un esquema existente requiere una actualización de base de datos planificada y no implica borrarla.

**Reseñas:** `resenas` vincula cada publicación con el destino y con el usuario autenticado. El dueño se determina desde el claim `uid` del JWT; se conserva texto, calificación de 1–5 e imagen binaria opcional (JPEG, PNG o WebP, máximo 5 MB). No se agregó un modelo de avatar.

### 4.7 Gestión administrativa (aprobado 2026-10-16, ADR-005)
- `usuarios.activo BOOLEAN NOT NULL DEFAULT TRUE`: `false` retira el acceso sin borrar la cuenta ni sus reservas.
- `auditoria_admin(id, admin_id NULL FK→usuarios ON DELETE SET NULL, accion, recurso, recurso_id NULL, detalle NULL, creada_en)`:
  registro de acciones administrativas. No almacena contraseñas, tokens ni datos personales.
