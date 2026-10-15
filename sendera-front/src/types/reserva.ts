export type EstadoReserva = 'PENDIENTE' | 'CONFIRMADA' | 'CANCELADA';

export interface Reserva {
    id: number;
    itinerarioId: number;
    fechaReserva: string; // Formato ISO: YYYY-MM-DD
    montoTotal: number;
    estadoReserva: EstadoReserva;
}

/** Cuerpo de POST /reservas según docs/openapi.yaml (CrearReservaDto). */
export interface CrearReservaRequest {
    itinerarioId: number;
    fechaReserva?: string;
}
