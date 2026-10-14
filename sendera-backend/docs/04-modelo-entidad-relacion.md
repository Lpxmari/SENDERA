# 4. Modelo entidad-relación (MER)

**Estado: PENDIENTE DE DISEÑO Y APROBACIÓN.**

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
- Usar migraciones versionadas (herramienta por decidir) y no cambiar producción manualmente.
- No serializar entidades JPA directamente en respuestas REST.
- Documentar cambios de esquema y su impacto en DTOs, servicios, frontend y pruebas.

## 4.4 Evidencia requerida para marcarlo como aprobado
- Diagrama MER revisado por el equipo.
- Diccionario de datos.
- Restricciones y reglas de integridad.
- Estados y transiciones para itinerarios/reservas/pagos.
- Aprobación explícita registrada en el historial del proyecto.
