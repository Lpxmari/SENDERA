---
name: security-audit
description: Audita autenticación, JWT, autorización por rol y recurso, contraseñas, validación, integraciones y manejo de datos sensibles en SENDERA.
---

# Auditoría de seguridad SENDERA

## Áreas
- Autenticación y respuestas ante credenciales inválidas.
- Hash de contraseñas y recuperación segura.
- Validación JWT: firma, expiración, emisor/audiencia si se configuran.
- Autorización de roles y propiedad de itinerarios/reservas.
- Endpoints administrativos.
- Validación DTO y protección contra asignación masiva.
- Inyección SQL y uso de consultas parametrizadas.
- CORS, HTTPS, secretos y configuración de despliegue.
- Logs: ausencia de contraseñas, tokens y datos de pago.
- Integraciones: timeout, reintentos, idempotencia y validación de callbacks.
- Formato de errores sin stack traces ni detalles internos.

## Método
1. Identifica superficie de ataque y endpoints.
2. Revisa evidencia en código y configuración.
3. Ejecuta o propone pruebas reproducibles.
4. Clasifica hallazgos por severidad e impacto.
5. Recomienda remediación concreta sin reescribir todo el sistema innecesariamente.

## Informe
Incluye resumen, hallazgos ordenados por severidad, evidencia, impacto, corrección y pruebas. Distingue problemas confirmados de aspectos no verificables por falta de configuración o código.
