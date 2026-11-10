# 6. Seguridad y autorización

## 6.1 Principios
- Aplicar mínimo privilegio y denegar por defecto.
- Validar autenticación, rol y propiedad del recurso en el backend.
- Tratar toda entrada del cliente como no confiable.
- No exponer secretos ni información interna en mensajes de error o logs.

## 6.2 Contraseñas
- Usar `PasswordEncoder` de Spring Security con BCrypt como mínimo, o Argon2 si se adopta y configura correctamente.
- Nunca cifrar de forma reversible ni almacenar contraseñas en texto plano.
- Aplicar política razonable de contraseñas y limitación de intentos.
- El flujo de recuperación debe usar tokens de alta entropía, expiración, uso único y invalidación después de completar el cambio.

## 6.3 JWT
- Firmar los tokens con algoritmo seguro y configuración explícita.
- Validar firma, expiración (`exp`), emisor (`iss`) y audiencia (`aud`) cuando se definan.
- Definir claims mínimos: identificador de usuario y roles/permisos necesarios; no incluir datos personales innecesarios.
- No confiar en roles o IDs de usuario enviados en el cuerpo, query string o estado del frontend.
- Definir duración de access token, renovación, revocación/cierre de sesión y efecto de la inactividad. La documentación menciona 15 minutos de inactividad: especificar el mecanismo para cumplirlo.
- No guardar JWT en logs. Evaluar el almacenamiento en frontend y las protecciones XSS/CSRF según el mecanismo escogido.

## 6.4 Autorización
- Usar Spring Security para proteger rutas y métodos.
- Definir matriz rol × acción antes de implementar endpoints.
- Comprobar propiedad de itinerarios y reservas. Conocer un ID no debe conceder acceso.
- Las acciones del administrador deben quedar restringidas y auditables.
- Si el acompañante puede operar sobre datos de un adulto mayor, definir relación, consentimiento, alcance y revocación del permiso.

## 6.5 Validación y ataques comunes
- Aplicar Bean Validation (`@Valid`, `@NotNull`, `@Size`, etc.) en DTOs y validaciones de negocio en servicios.
- Usar JPA/consultas parametrizadas; evitar construir SQL con concatenación de entrada.
- Configurar CORS con orígenes explícitos en producción.
- Limitar intentos de autenticación y recuperación. El inicio de sesión ya está limitado (ver §6.8); la recuperación de contraseña sigue pendiente.
- Proteger endpoints contra asignación masiva: mapear explícitamente los campos que el usuario puede modificar.
- Validar callbacks/webhooks externos según la documentación del proveedor.

## 6.6 Pagos y datos personales
- No almacenar número completo de tarjeta ni CVV.
- Mantener claves de integración en variables de entorno o gestor de secretos.
- No registrar contraseñas, tokens, datos completos de pago ni datos personales innecesarios.
- Enmascarar identificadores y limitar acceso a logs.

## 6.7 Checklist obligatorio por endpoint
- [ ] ¿Requiere autenticación?
- [ ] ¿Qué roles pueden ejecutarlo?
- [ ] ¿Se valida la propiedad del recurso?
- [ ] ¿Se valida el DTO en servidor?
- [ ] ¿Se limita la exposición de datos?
- [ ] ¿Se prueban respuestas 401, 403 y 404?
- [ ] ¿Los logs excluyen secretos?

## 6.8 Límite de intentos de inicio de sesión
Responde a los hallazgos OB-02, OM-04 y OM-07 de la auditoría externa: `POST /api/v1/auth/login` era vulnerable a fuerza bruta.

| Regla | Valor por defecto | Variable de entorno |
|---|---|---|
| Intentos por minuto por la combinación IP + correo | 5 | `LOGIN_INTENTOS_POR_MINUTO` |
| Intentos por minuto por IP (evita esquivar el límite cambiando de correo) | 20 | `LOGIN_INTENTOS_POR_IP_POR_MINUTO` |

- **Alcance:** solo `POST /auth/login`. No afecta a `/auth/registro`, `/destinos` ni otros endpoints.
- **Qué cuenta:** cada petición de login, acierte o falle. Los dos valores están en `application.yaml` (`app.seguridad.login.*`) y deben ser al menos 1.
- **Correo:** se normaliza con `trim` y minúsculas, así que `Ana@Correo.com ` y `ana@correo.com` comparten balde.
- **IP:** primer valor de `X-Forwarded-For` (Railway está detrás de un proxy); si falta o no parece una IP, `getRemoteAddr()`.
- **Respuesta al exceder:** `429` con el formato `ApiError` (vía `GlobalExceptionHandler`), el mensaje «Demasiados intentos de inicio de sesión. Espera un minuto e inténtalo de nuevo.» y la cabecera `Retry-After` con los segundos restantes (expuesta por CORS). Un intento rechazado por el límite de correo no gasta el cupo de la IP.
- **Implementación:** `LimiteLoginFilter` (HTTP: IP y lectura del correo) en la cadena de Spring Security y `LimiteLoginService` (reglas) con Bucket4j (`bucket4j-core`, algoritmo token bucket). Los baldes viven en memoria; los inactivos se eliminan como máximo una vez por minuto para que el mapa no crezca sin límite.
- **Registros:** solo IP y correo enmascarado (`a***@dominio.com`); nunca la contraseña.
- **Pruebas:** `LimiteLoginApiTest` y `LimiteLoginServiceTest`.

**Limitaciones conocidas (pendientes de decisión del equipo):**
- El estado está en memoria de cada instancia: con más de una réplica cada una cuenta por separado, y un reinicio borra los contadores.
- `X-Forwarded-For`: si el proxy no sobrescribe o antepone la cabecera del cliente, un atacante puede falsear el primer valor y rotar de IP. Confirmar el comportamiento de Railway antes de depender de este límite como única defensa.
- La recuperación de contraseña (`/auth/recuperar-contrasena`, `/auth/verificar-codigo`) aún no tiene límite propio.
