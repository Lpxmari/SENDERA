/** Itinerario reservable de un destino (GET /destinos/{id}/itinerarios, docs/openapi.yaml). */
export interface ItinerarioDisponible {
    id: number;
    nombre: string;
    fechaInicio: string; // YYYY-MM-DD
    fechaFin: string; // YYYY-MM-DD
    precioBase: number;
    cuposDisponibles: number;
}
