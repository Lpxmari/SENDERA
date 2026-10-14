# ADR-002: Autenticación JWT y autorización en backend

- **Estado:** Propuesto
- **Fecha:** Por completar
- **Decisores:** Equipo SENDERA

## Contexto
SENDERA tiene roles diferenciados y recursos privados como perfiles, itinerarios y reservas. La API debe proteger las operaciones sin confiar en el cliente.

## Decisión propuesta
Usar Spring Security para autenticación y autorización, con JWT firmado si se confirma como mecanismo de sesión. Validar firma, expiración y claims necesarios; comprobar rol y propiedad del recurso en backend.

## Alternativas
- Sesión tradicional del servidor.
- Autenticación implementada solo en frontend.
- JWT sin controles de autorización por recurso.

## Consecuencias
- Se deben definir duración, renovación, revocación y almacenamiento del token.
- Debe existir una matriz rol × acción y pruebas de 401/403.
- No se deben registrar tokens ni aceptar roles enviados por el cliente.
- La regla documentada de 15 minutos de inactividad debe concretarse técnicamente.
