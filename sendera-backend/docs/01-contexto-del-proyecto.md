# 1. Contexto del proyecto SENDERA

**Estado:** borrador basado en la documentación del proyecto.  
**Propósito:** ofrecer a Claude Code contexto estable sobre el producto.

## 1.1 Descripción
SENDERA es una aplicación web de turismo orientada principalmente a adultos mayores. Centraliza la búsqueda, comparación y organización de destinos y actividades turísticas, y permite gestionar reservas. La información debe facilitar decisiones según precio, duración, dificultad, ubicación y accesibilidad.

## 1.2 Problema
La información turística suele estar dispersa entre sitios web, redes sociales, agencias y operadores. Esto dificulta comparar alternativas y organizar actividades, horarios y reservas, especialmente cuando se necesita información clara sobre accesibilidad.

## 1.3 Objetivo general
Desarrollar una plataforma accesible que facilite a adultos mayores y sus acompañantes consultar opciones turísticas, organizar itinerarios y gestionar reservas de forma clara y segura.

## 1.4 Usuarios y actores
- **Adulto mayor:** consulta destinos y actividades, aplica filtros, organiza itinerarios y consulta o gestiona sus reservas.
- **Acompañante o familiar:** apoya la búsqueda, selección y reserva, de acuerdo con los permisos definidos.
- **Administrador:** gestiona usuarios, destinos, actividades y reservas según las autorizaciones asignadas.
- **Proveedor turístico:** suministra información de destinos, actividades o disponibilidad mediante el mecanismo de integración que se acuerde.

La existencia de un rol no implica acceso irrestricto. Las capacidades concretas deben definirse en requisitos y reglas de autorización.

## 1.5 Procesos principales
1. Gestión de usuarios y autenticación.
2. Consulta y gestión de destinos y actividades turísticas.
3. Gestión de itinerarios turísticos.
4. Gestión de reservas y, si se confirma en el alcance, coordinación de pagos.
5. Integración con servicios externos de turismo y notificaciones.

## 1.6 Alcance incluido según la documentación
- Registro, inicio de sesión y recuperación de contraseña.
- Gestión del perfil y control de roles.
- Consulta, búsqueda y filtrado de destinos y actividades por precio, duración, dificultad y accesibilidad.
- Visualización de detalles de actividades.
- Creación, organización y consulta de itinerarios.
- Consulta de disponibilidad y creación, consulta y cancelación de reservas.
- Administración de usuarios, destinos, actividades y reservas.
- Interfaz clara, sencilla y accesible.

## 1.7 Fuera del alcance
- Prestación física del servicio turístico.
- Transporte de usuarios a destinos.
- Gestión interna de hoteles, agencias u operadores.
- Inventarios propios de los proveedores.
- Servicios médicos o asistencia presencial durante actividades.

## 1.8 Stack tecnológico
- Frontend: React + TypeScript.
- Backend: Java + Spring Boot.
- API: REST sobre HTTPS.
- Base de datos: MySQL relacional, acceso mediante Spring Data JPA/Hibernate.
- Integraciones previstas: proveedor turístico, pasarela de pagos y servicio de notificaciones/correo.

## 1.9 Glosario
- **Destino turístico:** lugar que puede ser consultado por un usuario.
- **Actividad turística:** experiencia que se puede consultar y, según disponibilidad, incorporar a un itinerario.
- **Itinerario:** organización de actividades de un viaje.
- **Reserva:** registro de una solicitud o confirmación de un servicio, según las reglas de negocio aprobadas.
- **MER:** modelo entidad-relación. Se encuentra pendiente de aprobación.
- **DTO:** objeto de transferencia de datos que define una entrada o salida de API.
- **JWT:** token firmado utilizado para transportar claims de autenticación/autorización; no sustituye las verificaciones de permisos del backend.

## 1.10 Decisiones pendientes
- Aprobar MER, cardinalidades y restricciones.
- Precisar si el proveedor turístico integra catálogo/disponibilidad o si el administrador registra los datos manualmente.
- Definir flujo de pago y estados de reserva.
- Definir qué puede hacer un acompañante sobre la cuenta y los datos del adulto mayor.
- Confirmar RNF y criterios de aceptación exactos.
