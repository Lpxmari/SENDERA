---
description: Manejo consistente de errores
paths:
  - "backend/**/*.java"
  - "frontend/src/**/*"
---

- El backend usa un manejador global de excepciones y el formato `ApiError` definido en `docs/07-contrato-api.md` y `docs/openapi.yaml`.
- Mantén los códigos y esquemas de error sincronizados entre OpenAPI, Spring Boot y TypeScript.
- Usa códigos HTTP consistentes; diferencia 401 de 403 y considera 409 para conflictos.
- No envíes stack traces, SQL, secretos o mensajes internos al cliente.
- El frontend debe manejar errores HTTP, de red y respuestas sin cuerpo.
- Con Axios, estrecha `unknown` usando `axios.isAxiosError`; no supongas que todo error tiene `response`.
- No silencies errores ni muestres mensajes técnicos al usuario final.
- Los mensajes de interfaz deben ser claros y accesibles.
