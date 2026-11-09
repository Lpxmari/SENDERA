export type EstadoReserva = 'PENDIENTE' | 'CONFIRMADA' | 'CANCELADA';

/** Estado del pago en Mercado Pago; es independiente del estado de la reserva. */
export type EstadoPago = 'PENDIENTE' | 'APROBADO' | 'RECHAZADO' | 'REEMBOLSADO';

export interface Reserva {
    id: number;
    itinerarioId: number;
    fechaReserva: string; // Formato ISO: YYYY-MM-DD
    montoTotal: number;
    estadoReserva: EstadoReserva;
    /** null mientras no se haya iniciado un pago. */
    estadoPago?: EstadoPago | null;
    destinoId?: number;
    destinoNombre?: string;
    itinerarioNombre?: string;
}

/** Cuerpo de POST /reservas según docs/openapi.yaml (CrearReservaDto). */
export interface CrearReservaRequest {
    itinerarioId: number;
    fechaReserva: string;
}

/** Respuesta de POST /reservas/{id}/pago: el navegador se redirige a urlPago (Checkout Pro de Mercado Pago). */
export interface PreferenciaPago {
    preferenciaId: string;
    urlPago: string;
}
