---
name: review-backend
description: Revisa cambios de backend SENDERA para detectar fallos de arquitectura, seguridad, errores, persistencia, convenciones y pruebas.
---

# Revisión de backend

1. Lee `CLAUDE.md` y las reglas relevantes.
2. Revisa cambios contra RF/RNF y `docs/openapi.yaml`.
3. Comprueba que rutas, parámetros, DTOs serializados, respuestas, códigos HTTP y errores coincidan con OpenAPI.
4. Comprueba responsabilidades de controller/service/repository/dto/entity.
5. Comprueba validación de entrada, autorización por rol y propiedad del recurso.
6. Comprueba JWT, hash de contraseñas, secretos, CORS, logs y protección de datos.
7. Comprueba errores globales, códigos HTTP y ausencia de detalles internos.
8. Comprueba convenciones Java y BD.
9. Comprueba que el código persistente no asuma un MER no aprobado.
10. Revisa pruebas existentes y faltantes.

## Formato de salida
Para cada hallazgo indica:
- Severidad: Crítica / Alta / Media / Baja.
- Archivo y ubicación.
- Problema y escenario de riesgo.
- Requisito o regla incumplida.
- Corrección concreta.
- Prueba recomendada.

No marques un hallazgo como confirmado sin evidencia del código revisado. Separa defectos de mejoras opcionales.
