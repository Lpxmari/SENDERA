---
description: Reglas de pruebas automatizadas
paths:
  - "**/src/test/**/*"
  - "**/sendera-front/src/**/*"
---

- Añade pruebas para el comportamiento nuevo y para regresiones relevantes.
- Prueba casos correctos, límites, entradas inválidas y errores de dependencias.
- En backend, cubre servicios, API, repositorios y seguridad según el cambio.
- En frontend, cubre estados de carga, éxito, vacío y error.
- No afirmes que una prueba pasó si no ejecutaste el comando.
- Registra comandos ejecutados y cualquier prueba que no pudo ejecutarse.
