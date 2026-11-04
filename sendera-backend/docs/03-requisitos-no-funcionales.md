# 3. Requisitos no funcionales

**Importante:** usar la lista completa del plan de calidad oficial del proyecto como fuente primaria. Este documento recoge criterios que aparecen en la documentación y los convierte en puntos verificables; no reemplaza la aprobación del equipo ni pretende reconstruir requisitos que no estén disponibles aquí.

> **Nota sobre la numeración.** Los ids RNF-01 a RNF-14 de esta tabla son **provisionales** de este repositorio y **no coinciden** con la numeración del Plan de Gestión de la Calidad oficial v1.2 (RNF-01 a RNF-30). Solo **RNF-26** usa el id oficial y definitivo (sección 4.6.1 del plan). Los ids provisionales no se renumeran sin una decisión del equipo; hasta entonces, no los cruces con el plan oficial por número.

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

## RNF-26 — Filtros de consulta (aprobado por el equipo)
Id oficial y definitivo del Plan de Gestión de la Calidad v1.2, sección 4.6.1.

| Campo | Valor |
|---|---|
| ID | RNF-26 |
| Característica ISO/IEC 25010 | Adecuación funcional |
| Categoría | Completitud funcional |
| Prioridad | Alta |
| Proceso | Gestión de destinos y actividades turísticas |
| Texto | El módulo de consulta debe ofrecer los filtros de precio, nivel de dificultad y accesibilidad para los destinos, y el filtro de duración para las actividades, modelada en minutos, con los rangos: hasta 60 min, 61 a 180 min y más de 180 min (límites inclusivos). La interfaz muestra la duración en horas y minutos. |
| Métrica | El 100 % de las consultas aplica correctamente los filtros de su tipo: precio, dificultad y accesibilidad en destinos; duración en actividades. |

Impacto en el contrato: `GET /destinos` ya no acepta `duracionMin`/`duracionMax`; `GET /actividades` acepta `duracion`. Trazabilidad: `09-trazabilidad-requisitos.md` (RF-007, RF-008, RNF-26) y pruebas en `08-estrategia-de-pruebas.md` §8.4.2.

## Completar antes de implementar
1. Copiar todos los RNF identificados en el plan de calidad oficial, conservando sus IDs y redacción.
2. Para cada RNF, documentar método de medición, ambiente, carga, umbral y evidencia.
3. No asignar umbrales nuevos como requisitos obligatorios sin aprobación.
4. Relacionar cada RNF con pruebas y módulos en `09-trazabilidad-requisitos.md`.
