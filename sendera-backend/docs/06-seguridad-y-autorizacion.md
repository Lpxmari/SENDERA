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
- Limitar intentos de autenticación y recuperación; evaluar rate limiting.
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
