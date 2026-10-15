---
description: Reglas React y TypeScript para SENDERA
paths:
  - "**/sendera-front/src/**/*"
---

- Componentes, interfaces, tipos y enums en `PascalCase`.
- Variables, funciones y nombres de servicios en `camelCase`.
- Manejadores de eventos UI con prefijo `handle`, por ejemplo `handleSubmit`.
- Organiza `src/` en `components/`, `pages/`, `services/`, `types/` y `utils/`.
- Usa `docs/openapi.yaml` como contrato para consumir el backend; no inventes rutas, parámetros ni estructuras de respuesta.
- Siempre que la configuración lo permita, genera tipos TypeScript y/o cliente API a partir de OpenAPI; evita duplicar manualmente tipos derivados del contrato.
- Si un tipo de dominio necesita lógica adicional, mantenla separada de los tipos generados para evitar perderla al regenerar.
- Declara tipos compartidos en `src/types/`; evita `any` cuando exista un tipo preciso.
- Especifica tipos de retorno en funciones públicas y servicios asíncronos.
- Usa `fetch...` o `get...` para consultas API y devuelve promesas tipadas.
- Trata los errores de red y HTTP; no dependas solo de validaciones del cliente.
- Prioriza accesibilidad: texto legible, controles claros, foco visible y mensajes sencillos.
