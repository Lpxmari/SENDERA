# 9. Matriz de trazabilidad

Completar la matriz durante el desarrollo. Los nombres de clases/endpoints son candidatos hasta implementarse.

| ID requisito | Caso de uso / flujo | Capa o módulo | Endpoint / componente | Prueba | Estado |
|---|---|---|---|---|---|
| RF-001 | Registro | controller/service/dto | `POST /api/v1/auth/registro` | `AuthServiceImplTest` (válido, duplicado) | Probado (unitaria) |
| RF-002 | Inicio de sesión | controller/service/config | `POST /api/v1/auth/login`, `GET /auth/me` | `AuthServiceImplTest`; `SeguridadApiTest` (vencido, manipulado, emisor ajeno). Sin cierre de sesión | En desarrollo |
| RF-003 | Recuperación | service/integration | `/api/v1/auth/recuperar-contrasena`, `/cambiar-contrasena` | `AuthServiceImplTest` | Probado (unitaria) |
| RF-004 | Perfil | controller/service/dto | Por definir (`PUT /auth/me`) | — | Pendiente |
| RF-005 | Roles | config/controller | Escritura de destinos solo `ROLE_ADMIN`; rol firmado en el claim `roles` | `SeguridadApiTest` (401/403/201, identidad tomada del token), `ReservaPropiedadIntegracionTest` (propiedad con H2) | Probado (sin pruebas contra MySQL) |
| RF-006 | Catálogo de destinos | controller/service/repository | `GET /api/v1/destinos`, `/destinos/{id}` | `DestinoServiceImplTest`, `SeguridadApiTest` | En desarrollo (sin paginación) |
| RF-007 | Catálogo de actividades | — | Sin implementar | — | Pendiente |
| RF-008 | Filtros | service/repository | Parámetros de `GET /destinos` | `DestinoServiceImplTest` (casos límite pendientes) | En desarrollo |
| RF-009/010 | Itinerarios | — | Sin endpoints (existe la entidad `Itinerario`) | — | Pendiente |
| RF-011 | Disponibilidad | service | Se valida el cupo al reservar | `ReservaServiceImplTest` | En desarrollo |
| RF-012 | Reservas | controller/service/repository | `/api/v1/reservas` | `ReservaServiceImplTest` (monto, cupos, doble cancelación, propiedad), `ReservaPropiedadIntegracionTest` | Probado (sin pruebas contra MySQL) |
| RF-014 | Pago, si aplica | integration/service | Proveedor externo | — | Pendiente |
| RNF-01 | Rendimiento | API/consultas | Catálogo | Prueba de carga con umbral aprobado | Pendiente |
| RNF-05 | Inactividad 15 min | seguridad | JWT de 24 h, sin refresh | — | Bloqueado (decisión) |
| RNF-07 | Accesibilidad | frontend | Componentes/vistas | Contraste y navegación por teclado | Pendiente |

## Estados sugeridos
`Pendiente`, `En análisis`, `En desarrollo`, `Implementado`, `Probado`, `Aceptado`, `Bloqueado`.

Mantener la matriz sincronizada con cambios en requisitos, código y pruebas.
