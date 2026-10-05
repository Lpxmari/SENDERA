export type EstadoReserva = 'PENDIENTE' | 'CONFIRMADA' | 'CANCELADA';

export interface Reserva {
    id: number;
    itinerarioId: number;
    fechaReserva: string; // Formato ISO: YYYY-MM-DD
    montoTotal: number;
    estadoPago: EstadoReserva;
}