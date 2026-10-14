---
description: Reglas de seguridad para SENDERA
paths:
  - "backend/**/*.java"
  - "frontend/src/**/*"
---

- Nunca almacenes contraseñas en texto plano; usa `PasswordEncoder` seguro.
- Nunca confíes en roles, IDs o permisos declarados por el frontend.
- Protege endpoints en Spring Security y valida propiedad del recurso.
- Valida firma, expiración y claims relevantes del JWT.
- No incluyas secretos, contraseñas, JWT, tokens de recuperación ni datos completos de pago en logs.
- Usa HTTPS en despliegue y CORS restringido en producción.
- Valida entradas en backend; la validación de frontend solo complementa.
- No almacenes números completos de tarjeta ni CVV.
- Añade pruebas de autenticación, autorización y acceso a recursos ajenos.
