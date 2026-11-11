# 13. Respuesta a la auditoría externa

Fecha: 10/11/2026 · Equipo SENDERA (Mariana Ramírez Colorado, Nicolás Buitrago Martínez, Jeffry Smith Ceballos Bejarano)

De los 17 hallazgos y oportunidades del informe, 13 quedan corregidos con evidencia, 1 parcialmente y 3 planificados con mitigaciones vigentes.

## 13.1 Estado de cada hallazgo

Los números de PR corresponden a este repositorio; desde el 10/11/2026 todo cambio pasa por el CI y la aprobación de otro integrante antes de fusionarse a `dev`.

| Hallazgo | Resumen | Acción tomada | Evidencia | Estado |
| --- | --- | --- | --- | --- |
| NC-01 | El perfil solo simula guardar los datos personales | Persistir identificación, fecha de nacimiento, país, favoritos e intereses en la base y conectar la pantalla a `GET/PUT /auth/me` | PR del perfil (Nicolás Buitrago) | Corregido |
| NC-02 | Imágenes de reseñas en LONGBLOB y de destinos por URL externa | Se limitó la causa del riesgo: una reseña por usuario y destino, solo con reserva confirmada; máximo 5 MB y validación de tipo y firma del archivo. La migración a almacenamiento de objetos queda planificada (ADR-006) | Restricción `uq_resenas_usuario_destino`; `ResenaReglasIntegracionTest` | Mitigado y planificado |
| NC-03 | Flujo de ramas, CI solo en `main`, ramas sin borrar y hashes en el historial | CI en `dev` y `main`; escaneo de secretos con gitleaks; `main` protegida (PR con 1 aprobación y checks obligatorios); ramas fusionadas borradas y borrado automático activo; contraseñas de las cuentas expuestas cambiadas el 10/11/2026 | PR #26; regla de protección de `main`; configuración del repositorio | Corregido |
| NC-04 | Proceso 3 publicado sin estabilizar | Se corrigieron los defectos del proceso 3 y todo cambio pasa por `feature → dev → main` con CI, aprobación, respaldo previo y pruebas de humo en el despliegue. Ver aclaración en 13.3 | PR #20, #26; `docs/12-respaldo-y-recuperacion.md` | Corregido |
| OB-01 | `GET /destinos` sin paginación | Paginación en la consulta (`page`, `size` por defecto 12 y máximo 50) y en las pantallas | PR #32; `DestinoPaginacionApiTest`, `DestinoPaginacionIntegracionTest` | Corregido |
| OB-02 | Sin límite de intentos en `/auth/login` | 5 intentos por minuto por IP y correo, y 20 por IP; responde 429 con `Retry-After` | PR #28 | Corregido |
| OB-03 | Rol PROVEEDOR en documentos | Retirado de la Definición, el Plan de Calidad, el C4 y UI SENDERA 2.0; el proveedor queda como sistema externo previsto | Documentos v1.3–v1.4 | Corregido |
| OB-04 | Manual con reservas que no existen y teléfono 911 | Manual reescrito con el flujo real: itinerario, fecha, pago, cancelación e itinerarios personalizados; se retira el 911 | Manual de usuario actualizado (Jeffry Ceballos) | Corregido |
| OB-05 | `destinoId` enviado como `itinerarioId` | Endpoint `GET /destinos/{id}/itinerarios` y selector de itinerario en el formulario | PR #20; `FormularioReserva.test.tsx`, `ItinerarioDisponibleIntegracionTest` | Corregido |
| OB-06 | Fechas pasadas en reservas y reseñas sin reserva | `@FutureOrPresent` y validación de la fecha dentro del itinerario; reseña solo con reserva confirmada | PR #20; `ReservaApiTest`, `ResenaReglasIntegracionTest` | Corregido |
| OM-01 | Selector de itinerarios | Implementado en el formulario de reserva | PR #20 | Implementado |
| OM-02 | Almacenamiento de objetos para imágenes | Planificado (ver NC-02) | ADR-006 | Planificado |
| OM-03 | JWT en cookie HttpOnly | Planificado. Mitigación actual: el token expira a las 24 h, React escapa el contenido y el frontend no usa `dangerouslySetInnerHTML` | `application.yaml` (`JWT_EXPIRACION_HORAS`) | Planificado |
| OM-04 | Limitador de tasa | Implementado en el inicio de sesión (Bucket4j) | PR #28 | Implementado |
| OM-05 | Análisis estático de seguridad en CI | gitleaks en cada push y PR; SonarCloud configurado en el CI (se activa con su token) | PR #26 | Implementado parcialmente |
| OM-06 | Dificultad y accesibilidad en el detalle del alojamiento | Se muestran junto al precio, con ícono y texto | PR #30; `DetalleDestino.test.tsx` | Implementado |
| OM-07 | Límite de intentos de inicio de sesión | Igual que OB-02 | PR #28 | Implementado |

## 13.2 Plan de mejoramiento actualizado

Las filas 1 a 6 son las del informe; las filas 7 a 9 son acciones que el equipo agregó por otros puntos de la lista de verificación. El informe no incluye una fila 5; se conserva su numeración.

| # | Hallazgo | Acción correctiva o preventiva | Responsable | Estado | Evidencia |
| --- | --- | --- | --- | --- | --- |
| 1 | Fechas pasadas en reservas | `min` en el calendario y `@FutureOrPresent` en `CrearReservaDto`; la fecha debe estar dentro del itinerario | Backend / Frontend | Cerrado | PR #20 |
| 2 | Cruce de identificadores | `GET /destinos/{id}/itinerarios` y selector en `FormularioReserva.tsx` | Backend / Frontend | Cerrado | PR #20 |
| 3 | Fuga de credenciales | Contraseñas cambiadas el 10/11/2026; gitleaks en el CI; credenciales retiradas de los documentos. `enlaces.txt` no existe en el historial del repositorio | Mariana Ramírez | Cerrado | PR #26 |
| 4 | DoS por imágenes en LONGBLOB | Una reseña por usuario y destino (cerrado); migración a almacenamiento de objetos (planificado) | Backend | Parcial | `uq_resenas_usuario_destino` |
| 6 | JWT en localStorage | Migración a cookie HttpOnly, Secure y SameSite | Frontend / Seguridad | Planificado | — |
| 7 | Sin copias de seguridad | Política de respaldo antes de cada paso a `main` y semanal, con prueba de restauración del 10/11/2026 | Mariana Ramírez | Cerrado | `docs/12-respaldo-y-recuperacion.md` |
| 8 | Sin procedimiento de rollback | Rollback por Railway y regla de ejecutar los scripts de esquema antes de pasar a `main` | Mariana Ramírez | Cerrado | `docs/12-respaldo-y-recuperacion.md` |
| 9 | Flujo de ramas y CI | CI en `dev`, `main` protegida, ramas fusionadas borradas | Mariana Ramírez | Cerrado | PR #26; regla de `main` |

## 13.3 Aclaraciones al informe

Estos puntos no cambian el compromiso de corregir, pero el equipo solicita que se tengan en cuenta al valorar los hallazgos.

| Punto del informe | Aclaración | Evidencia |
| --- | --- | --- |
| Credenciales y teléfono en los documentos (riesgo 4, plan #3) | Se incluyeron a solicitud del equipo auditor para acceder al despliegue, como reconoce el propio informe en la lista de verificación de seguridad. Ya se retiraron y las contraseñas se cambiaron | Lista de verificación 6.7, ítem 3 |
| Despliegue prematuro del proceso 3 (NC-04, OB-06) | En los documentos entregados para la auditoría se informó que el proceso 3 no estaba implementado, y el informe lo reconoce en OB-06. Se veía en el despliegue porque el proyecto tiene un solo ambiente, no porque se presentara como terminado | Definición del Proyecto y Diseño de Pruebas entregados; OB-06 |
| Versión de Spring Boot (F-01) | El proyecto usa Spring Boot 3.3.4, no 3.4.5 | `sendera-backend/build.gradle` |
| Clave JWT por defecto (riesgo 3) | El valor por defecto existía solo en el perfil de desarrollo, nunca en producción, y ya se eliminó | `application-dev.yaml` |
| Sobreventa y lógica transaccional del proceso 3 | La reserva y la cancelación ya usaban bloqueo pesimista sobre el itinerario dentro de una transacción, lo que impide la sobreventa de cupos | `ItinerarioRepository.buscarParaReservar` (`PESSIMISTIC_WRITE`) |
| Reseñas «infinitas» y colapso de la base | Las imágenes ya tenían límite de 5 MB y validación de tipo y contenido. Hoy además se permite una sola reseña por usuario y destino, y solo con reserva confirmada | `ResenaServiceImpl` |
| Pruebas de aceptación (6.5) | La justificación menciona concurrencia y estrés, que son pruebas no funcionales; están diseñadas como PN03 y PN10 | Diseño, Reutilización y Pruebas, sección 9 |

## 13.4 Matriz de riesgos

Responde al punto 6.1 de la lista de verificación (gestión de riesgos). Se revisa antes de cada paso a `main`; el nivel es la combinación de impacto y probabilidad después de la mitigación actual.

| ID | Riesgo | Tipo | Impacto | Probabilidad | Nivel | Mitigación actual | Acción pendiente | Responsable |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| R1 | Pérdida de datos de producción (el plan Hobby de Railway no tiene backups automáticos) | Operativo | Alto | Baja | Medio | Respaldo antes de cada paso a `main` y semanal; prueba de restauración del 10/11/2026 | Automatizar el respaldo diario | Mariana Ramírez |
| R2 | Despliegue fallido por un cambio de esquema no aplicado (ocurrió el 10/11/2026 con la tabla `pagos`) | Operativo | Medio | Media | Medio | Regla: ejecutar los scripts `docs/actualizar-*.sql` antes de pasar a `main`; Railway mantiene la versión anterior si falla el chequeo de salud | — | Mariana Ramírez |
| R3 | Fuerza bruta en el inicio de sesión | Seguridad | Alto | Baja | Medio | Límite de intentos con respuesta 429; contraseñas con BCrypt | — | Mariana Ramírez |
| R4 | Robo del token por XSS (token en localStorage) | Seguridad | Alto | Baja | Medio | Expiración de 24 h; React escapa el contenido; sin `dangerouslySetInnerHTML` | Migrar a cookie HttpOnly | Equipo |
| R5 | Exposición de secretos en el repositorio | Seguridad | Alto | Baja | Medio | gitleaks en cada push y PR; secretos solo en variables de Railway y GitHub | — | Mariana Ramírez |
| R6 | Saturación de la base por imágenes de reseñas | Operativo | Medio | Baja | Bajo | Máximo 5 MB; una reseña por usuario y destino con reserva confirmada | Almacenamiento de objetos | Equipo |
| R7 | Falla o cambio de la API de Mercado Pago | Integración | Medio | Media | Medio | La reserva queda PENDIENTE; el pago se verifica al volver y por webhook con firma | — | Nicolás Buitrago |
| R8 | Falla del servicio de correo (Brevo) | Integración | Medio | Media | Medio | El registro y la recuperación no fallan para el usuario; el error queda en el log | Reintento automático | Equipo |
| R9 | Servicios detenidos por saldo o pago fallido en Railway | Operativo | Alto | Baja | Medio | Tope de gasto y tarjeta registrada | — | Mariana Ramírez |
| R10 | Sobreventa de cupos con reservas simultáneas | Funcional | Alto | Baja | Medio | Bloqueo pesimista sobre el itinerario en una transacción | Ejecutar PN10 con JMeter | Equipo |
| R11 | Adultos mayores que no logran completar las tareas | Usabilidad | Medio | Media | Medio | Interfaz de alto contraste; Lighthouse 95 en accesibilidad | Repetir Lighthouse en cada entrega | Jeffry Ceballos |
| R12 | Un solo ambiente (sin pruebas previas a producción) | Operativo | Medio | Media | Medio | CI y aprobación en cada PR; pruebas de humo en el despliegue | Ambiente de pruebas en Railway | Equipo |