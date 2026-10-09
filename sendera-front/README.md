# SENDERA

Sendera es una propuesta de software que busca facilitar la búsqueda, selección y reserva de destinos y actividades turísticas para adultos mayores. La plataforma permitirá consultar información relevante sobre los destinos, identificar actividades según su dificultad, duración y accesibilidad, y realizar reservas de manera sencilla.

El sistema tiene como objetivo reunir en una sola plataforma los procesos de gestión de usuarios, consulta de opciones turísticas y seguimiento de reservas. Así, se proporcionará una experiencia clara, accesible y segura. De esta forma, se espera apoyar a los adultos mayores y sus acompañantes en la planificación de sus viajes, así como a los proveedores de los servicios turísticos.


# SENDERA Backend API

API RESTful desarrollada para la plataforma **SENDERA**, orientada a la gestión de destinos turísticos y la reserva de experiencias ecoturísticas.

El proyecto está diseñado bajo un enfoque de **Arquitectura Multicapa (Layered Architecture)** y sigue las mejores prácticas de desarrollo backend en Java con Spring Boot.

---

## Tecnologías y Herramientas

* **Lenguaje:** Java 21
* **Framework:** Spring Boot 3.3.4
* **Persistencia:** JPA / Hibernate
* **Base de Datos:** MySQL
* **Construcción y Dependencias:** Gradle
* **Librerías Auxiliares:** Lombok, Jakarta Validation
* **Control de Versiones:** Git & GitHub (Estrategia GitFlow)

---

## Arquitectura del Proyecto

El sistema aplica una separación clara de responsabilidades:

```text
com.sendera
├── controller   # Exposición de endpoints REST (HTTP Request/Response)
├── service      # Lógica de negocio y reglas del sistema
├── repository   # Métodos de acceso y consultas a la base de datos (JPA)
├── dto          # Transferencia de datos y reglas de validación (@Valid)
├── model        # Entidades JPA mapeadas a las tablas de la BD
└── exception    # Manejo global de excepciones

---

## Gestión de usuarios y autenticación

Autenticación con **JWT (HS256)** sin estado, contraseñas con **BCrypt** y recuperación por correo con token de un solo uso (se guarda solo su hash SHA-256 y vence a los 30 minutos).

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/api/v1/auth/registro` | Público | Crea la cuenta y devuelve `{token, usuario}` |
| POST | `/api/v1/auth/login` | Público | Inicia sesión y devuelve `{token, usuario}` |
| POST | `/api/v1/auth/recuperar-contrasena` | Público | Envía el enlace de recuperación (204 siempre) |
| POST | `/api/v1/auth/cambiar-contrasena` | Público | Cambia la contraseña con el token del correo |
| GET | `/api/v1/auth/me` | Con sesión | Datos del usuario autenticado |

**Reglas de acceso:** `GET /destinos/**` es público; el resto de la API (reservas, crear destinos) requiere `Authorization: Bearer <token>`.

**Base de datos:** ejecuta `database/migrations/001_usuarios_y_autenticacion.sql` si ya tienes la BD creada (agrega `usuarios` y `tokens_recuperacion`). `ddl-auto` está en `validate`, así que sin estas tablas el backend no arranca.

**Variables de entorno** (ver `.env.example`): `JWT_SECRET` (mín. 32 caracteres, obligatoria en producción), `FRONTEND_URL`, y `SPRING_MAIL_HOST/PORT/USERNAME/PASSWORD` para el envío real de correos. Sin SMTP configurado, el enlace de recuperación se imprime en la consola del backend (solo desarrollo).
