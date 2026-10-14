# 3. Requisitos no funcionales

**Importante:** usar la lista completa del plan de calidad oficial del proyecto como fuente primaria. Este documento recoge criterios que aparecen en la documentación y los convierte en puntos verificables; no reemplaza la aprobación del equipo ni pretende reconstruir requisitos que no estén disponibles aquí.

| ID provisional | Atributo | Criterio documentado / por validar |
|---|---|---|
| RNF-01 | Rendimiento | Las consultas del catálogo deberían responder en menos de 2 segundos bajo la carga y el entorno de prueba que defina el equipo. Precisar percentil, volumen y condiciones. |
| RNF-02 | Seguridad | La comunicación en despliegue debe usar HTTPS. |
| RNF-03 | Seguridad | Las contraseñas se almacenan con un algoritmo de hash adecuado; nunca en texto plano. |
| RNF-04 | Seguridad | Los endpoints aplican autenticación y autorización en el servidor. |
| RNF-05 | Seguridad | La sesión debe contemplar cierre por inactividad; la documentación menciona 15 minutos. Definir cómo se aplica con JWT y cómo se renuevan/revocan tokens. |
| RNF-06 | Pagos | SENDERA no almacena datos completos de tarjetas; el procesamiento corresponde a la pasarela externa. |
| RNF-07 | Accesibilidad | Contraste mínimo de 4.5:1 para texto normal, sujeto a la norma/criterio adoptado. |
| RNF-08 | Mantenibilidad | Código modular, responsabilidades claras y separación por capas. |
| RNF-09 | Pruebas | Automatizar pruebas apropiadas de lógica, integración y seguridad. Definir cobertura objetivo solo si el equipo la aprueba. |
| RNF-10 | Documentación | API documentada, por ejemplo con OpenAPI, y decisiones arquitectónicas registradas. |
| RNF-11 | Integridad | Validar conflictos de horario y disponibilidad en itinerarios/reservas cuando corresponda a las reglas aprobadas. |
| RNF-12 | Usabilidad | Interfaz sencilla, mensajes claros, controles legibles y navegación consistente para adultos mayores. |
| RNF-13 | Fiabilidad | Manejar fallos y timeouts de servicios externos sin filtrar información sensible. |
| RNF-14 | Compatibilidad | Interfaz web adaptable a los tamaños de pantalla que el equipo incluya en sus pruebas. |

## Completar antes de implementar
1. Copiar todos los RNF identificados en el plan de calidad oficial, conservando sus IDs y redacción.
2. Para cada RNF, documentar método de medición, ambiente, carga, umbral y evidencia.
3. No asignar umbrales nuevos como requisitos obligatorios sin aprobación.
4. Relacionar cada RNF con pruebas y módulos en `09-trazabilidad-requisitos.md`.
