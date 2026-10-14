# 9. Matriz de trazabilidad

Completar la matriz durante el desarrollo. Los nombres de clases/endpoints son candidatos hasta implementarse.

| ID requisito | Caso de uso / flujo | Capa o módulo | Endpoint / componente | Prueba | Estado |
|---|---|---|---|---|---|
| RF-001 | Registro | controller/service/dto | `POST /api/v1/usuarios` | Registro válido, inválido y duplicado | Pendiente |
| RF-002 | Inicio/cierre de sesión | controller/service/config | `/api/v1/auth/...` | Token válido, inválido y expirado | Pendiente |
| RF-003 | Recuperación | service/integration | `/api/v1/auth/recuperacion` | Token expirado, usado y válido | Pendiente |
| RF-004 | Perfil | controller/service/dto | Por definir | Actualización permitida y campo prohibido | Pendiente |
| RF-005 | Roles | config/controller/service | Endpoints protegidos | 401/403 y prueba de acceso a recurso ajeno | Pendiente |
| RF-006 | Catálogo de destinos | controller/service/repository | `/api/v1/destinos-turisticos` | Lista, detalle, paginación y 404 | Pendiente |
| RF-007 | Catálogo de actividades | controller/service/repository | `/api/v1/actividades-turisticas` | Lista y detalle | Pendiente |
| RF-008 | Filtros | service/repository | Catálogos | Filtros combinados y límites | Pendiente |
| RF-009/010 | Itinerarios | controller/service/repository | `/api/v1/itinerarios` | CRUD permitido, conflicto horario y propiedad | Pendiente |
| RF-011/012 | Reservas | controller/service/integration | `/api/v1/reservas` | Disponibilidad, cancelación, duplicado y permisos | Pendiente |
| RF-014 | Pago, si aplica | integration/service | Proveedor externo | Timeout, callback inválido e idempotencia | Pendiente |
| RNF-01 | Rendimiento | API/consultas | Catálogo | Prueba de carga con umbral aprobado | Pendiente |
| RNF-07 | Accesibilidad | frontend | Componentes/vistas | Contraste y navegación por teclado | Pendiente |

## Estados sugeridos
`Pendiente`, `En análisis`, `En desarrollo`, `Implementado`, `Probado`, `Aceptado`, `Bloqueado`.

Mantener la matriz sincronizada con cambios en requisitos, código y pruebas.
