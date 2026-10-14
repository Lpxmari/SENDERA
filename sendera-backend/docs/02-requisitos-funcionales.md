# 2. Requisitos funcionales

**Estado:** catálogo inicial por validar con el equipo. Los IDs deben mantenerse estables una vez aprobados. No interpretar ejemplos como autorización para implementar decisiones pendientes.

## RF-001 — Registro de usuario
El sistema permitirá registrar usuarios solicitando únicamente los datos necesarios y validando su formato y obligatoriedad en el backend.  
**Aceptación:** datos inválidos producen una respuesta de validación clara; la contraseña se almacena solo como hash; no se filtran detalles internos.

## RF-002 — Inicio y cierre de sesión
El sistema permitirá autenticarse con credenciales válidas y finalizar la sesión conforme al mecanismo de tokens adoptado.  
**Aceptación:** credenciales incorrectas no revelan si una cuenta existe; endpoints protegidos rechazan solicitudes sin autenticación válida.

## RF-003 — Recuperación de contraseña
El sistema permitirá iniciar un flujo seguro de recuperación.  
**Aceptación:** los tokens de recuperación son aleatorios, de uso limitado, expiran y no se almacenan ni registran en texto plano cuando el diseño permita almacenar su hash.

## RF-004 — Gestión de perfil
El usuario podrá consultar y actualizar los campos de su perfil que tenga autorizados.  
**Aceptación:** se validan los datos y el usuario no puede modificar su rol mediante una actualización ordinaria del perfil.

## RF-005 — Control de roles y permisos
El backend autorizará cada operación según el rol y los permisos del usuario autenticado.  
**Aceptación:** la autorización se prueba en endpoints y recursos; ocultar botones en frontend no cuenta como control de acceso.

## RF-006 — Consulta de destinos
El sistema permitirá listar y consultar el detalle de destinos disponibles.  
**Aceptación:** la API usa DTOs, paginación cuando corresponda y respuestas HTTP documentadas.

## RF-007 — Consulta de actividades
El sistema permitirá listar actividades y consultar sus detalles, incluyendo información disponible de duración, dificultad, precio y accesibilidad.  
**Aceptación:** los campos no confirmados por el MER o por el proveedor no se inventan; su definición se registra como pendiente.

## RF-008 — Búsqueda y filtros
El usuario podrá filtrar opciones por criterios aprobados como precio, duración, dificultad y accesibilidad.  
**Aceptación:** combinaciones de filtros se validan; consultas son parametrizadas y se prueban casos límite.

## RF-009 — Creación y edición de itinerarios
El usuario podrá crear y modificar itinerarios propios con las actividades seleccionadas, sujeto a las reglas aprobadas.  
**Aceptación:** el sistema valida datos, permisos de propiedad y conflictos de horarios cuando esa regla aplique.

## RF-010 — Consulta de itinerario
El usuario podrá ver el detalle de sus itinerarios y sus actividades organizadas.  
**Aceptación:** un usuario no obtiene itinerarios ajenos salvo que exista una autorización explícita para acompañantes.

## RF-011 — Consulta de disponibilidad
El sistema permitirá consultar disponibilidad antes de confirmar una reserva, de acuerdo con la fuente de datos adoptada.  
**Aceptación:** se define cómo tratar información externa desactualizada y errores del proveedor.

## RF-012 — Gestión de reservas
El usuario podrá crear, consultar y cancelar reservas conforme a las políticas del servicio.  
**Aceptación:** se validan disponibilidad, estado, permisos y reglas de cancelación; las transiciones de estado quedan documentadas.

## RF-013 — Gestión administrativa
El administrador podrá gestionar usuarios, destinos, actividades y reservas dentro de los permisos definidos.  
**Aceptación:** operaciones administrativas quedan protegidas en backend y registran auditoría sin datos sensibles.

## RF-014 — Integración de pagos
Si el pago electrónico se confirma dentro del alcance, el backend coordinará el pago mediante una pasarela externa.  
**Aceptación:** SENDERA no almacena números completos de tarjeta ni códigos de seguridad; se validan callbacks/webhooks según el proveedor.

## RF-015 — Notificaciones
El sistema podrá enviar notificaciones de eventos relevantes, como recuperación de contraseña o cambios de reserva, mediante un servicio dedicado.  
**Aceptación:** fallos externos se manejan con reintentos controlados y no invalidan indebidamente la operación principal.

## RF-016 — Accesibilidad de interfaz
La interfaz ofrecerá navegación clara, texto legible, mensajes comprensibles y controles utilizables por adultos mayores.  
**Aceptación:** se verifican los criterios de accesibilidad definidos en los RNF y mediante pruebas de usabilidad.

## Registro de requisitos
Por cada requisito aprobado, añadir: prioridad, actor, precondiciones, flujo principal, excepciones, reglas de negocio, criterios de aceptación y pruebas vinculadas.
