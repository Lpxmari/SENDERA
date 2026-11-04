import { EstadoReserva } from '../types/reserva';

interface EtiquetaEstado {
    /** Texto completo que se muestra y lee el lector de pantalla; el estado nunca depende solo del color. */
    texto: string;
    /** Adorno visual extra (se oculta a los lectores de pantalla). */
    simbolo: string;
}

export const ETIQUETAS_ESTADO: Record<EstadoReserva, EtiquetaEstado> = {
    PENDIENTE: { texto: 'Pendiente de confirmación', simbolo: '⏳' },
    CONFIRMADA: { texto: 'Confirmada', simbolo: '✓' },
    CANCELADA: { texto: 'Cancelada', simbolo: '✕' },
};
