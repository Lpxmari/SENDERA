# Convenciones de integración: base de datos, backend y frontend

Este documento consolida el manual compartido por el equipo. Ante cualquier cambio, actualizar este archivo y las reglas correspondientes de `.claude/rules/`.

## 1. Naming conventions

| Área | Convención | Ejemplos |
|---|---|---|
| Base de datos | Tablas plurales, minúsculas y `snake_case`; columnas `snake_case` | `destinos_turisticos`, `fecha_reserva`, `destino_id` |
| Java/Spring | Clases `PascalCase`; métodos y variables `camelCase` | `ReservaController`, `crearReserva`, `destinoId` |
| React/TypeScript | Componentes, interfaces y tipos `PascalCase`; variables/funciones `camelCase` | `TarjetaDestino.tsx`, `Destino`, `handleSubmit` |
| Archivos de servicios frontend | `camelCase` | `destinoService.ts`, `reservaService.ts` |
| API REST | Rutas plurales, minúsculas, `kebab-case`, versionadas | `/api/v1/destinos-turisticos` |

No confundir nombres de columnas con propiedades JSON. La persistencia usa `snake_case`; Java y TypeScript usan `camelCase` de forma predeterminada, salvo que el contrato API defina otra política.

## 2. Tipos TypeScript
Ubicar interfaces y tipos compartidos en `src/types/`. Ejemplos ilustrativos:

```ts
// src/types/destino.ts
export interface Destino {
  id: number;
  nombre: string;
  descripcion: string;
  ubicacion: string;
  precioBase: number;
  fotosUrl: string[];
}

export type CrearDestinoDto = Omit<Destino, 'id'>;
```

```ts
// src/types/reserva.ts
export type EstadoReserva = 'PENDIENTE' | 'CONFIRMADA' | 'CANCELADA';

export interface Reserva {
  id: number;
  itinerarioId: number;
  fechaReserva: string; // ISO YYYY-MM-DD si es solo fecha
  montoTotal: number;
  estadoReserva: EstadoReserva;
}
```

Los ejemplos no son esquema aprobado: revisar los campos con el MER y contrato API. El manual original usaba `estadoPago: EstadoReserva`; se recomienda no reutilizar el tipo de estado de reserva para un campo de pago. Si el pago es parte del alcance, definir un `EstadoPago` independiente.

```ts
// src/types/api.ts
export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}
```

## 3. Nombres de métodos y funciones
### Backend
- Consulta: `obtenerDestinoPorId`, `listarDestinos`.
- Creación: `crearReserva`, `guardarDestino`.
- Actualización: `actualizarEstadoReserva`.
- Eliminación: `eliminarItinerario`.

Mantener un idioma coherente dentro del proyecto y usar nombres que expresen intención. No es obligatorio traducir nombres de métodos de frameworks.

### Frontend
- Manejadores de UI: `handleSubmit`, `handleClickDestino`.
- Servicios API: `fetchDestinos(): Promise<Destino[]>`, `getDestinoPorId(...)`.
- Especificar retornos de funciones públicas y tipos de datos asíncronos.

## 4. Estructura frontend

```text
src/
├── components/       # Componentes reutilizables
│   └── TarjetaDestino.tsx
├── pages/            # Vistas principales
│   ├── Home.tsx
│   └── DetalleDestino.tsx
├── services/         # Consumo de API
│   ├── destinoService.ts
│   └── reservaService.ts
├── types/            # Interfaces y tipos
│   ├── destino.ts
│   ├── reserva.ts
│   └── api.ts
└── utils/             # Funciones auxiliares
```

## 5. Estructura backend

```text
src/main/java/com/sendera/
├── controller/
├── service/
├── repository/
├── entity/
├── dto/
├── exception/
├── config/
└── integration/
```

## 6. Contrato estándar de errores
La API debe devolver un formato consistente compatible con `ApiError`. Ejemplo:

```json
{
  "timestamp": "2026-10-14T15:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "La solicitud contiene datos inválidos.",
  "path": "/api/v1/destinos-turisticos"
}
```

El frontend debe tratar el error como `AxiosError<ApiError>` cuando use Axios, verificar que `response` exista y mostrar un mensaje seguro y útil. No registrar tokens, credenciales ni datos personales sensibles. No asumir que toda excepción de red tiene `response`.

## 7. Códigos HTTP
- `200 OK`: consulta o actualización con cuerpo.
- `201 Created`: creación exitosa.
- `204 No Content`: operación exitosa sin cuerpo, si se adopta.
- `400 Bad Request`: solicitud inválida o campos obligatorios ausentes.
- `401 Unauthorized`: falta autenticación válida.
- `403 Forbidden`: autenticado, pero sin permisos.
- `404 Not Found`: recurso inexistente o no accesible según la política de revelación.
- `409 Conflict`: conflicto con el estado actual.
- `500 Internal Server Error`: error inesperado, sin detalles internos.

## 8. Ejemplo de consumo API tipado
```ts
import axios, { AxiosError } from 'axios';
import type { Destino } from '../types/destino';
import type { ApiError } from '../types/api';

export const fetchDestinos = async (): Promise<Destino[]> => {
  try {
    const response = await axios.get<Destino[]>('/api/v1/destinos-turisticos');
    return response.data;
  } catch (error: unknown) {
    if (axios.isAxiosError<ApiError>(error) && error.response) {
      // Mostrar o propagar un mensaje seguro desde la capa de UI.
      // No registrar tokens ni datos sensibles.
      throw new Error(error.response.data.message || 'No fue posible consultar los destinos.');
    }
    throw new Error('No fue posible conectar con el servidor.');
  }
};
```

Preferir `unknown` en el `catch` y estrechar el tipo con `axios.isAxiosError` en vez de asumir que cualquier valor es un `AxiosError`. La capa de servicio puede propagar un error tipado; los componentes presentan mensajes accesibles y no deben silenciar el fallo.
