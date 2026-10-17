import { EstadoReserva } from './reserva';

/**
 * Perfil del viajero. PROPUESTA de contrato: aún no existe en docs/openapi.yaml ni en el MER,
 * por lo que hoy los datos son simulados (ver services/perfilService.ts).
 */
export interface PerfilViajero {
    nombreUsuario: string;
    nivelViajero: string;
    fotoUrl: string | null;
    numeroIdentificacion: string;
    nombre: string;
    apellidos: string;
    fechaNacimiento: string; // ISO: YYYY-MM-DD
    correo: string;
    paisResidencia: string;
    destinosFavoritos: string[];
    intereses: string[];
}

/** Campos que la persona puede editar desde la vista de perfil. */
export type PerfilEditable = Omit<PerfilViajero, 'nombreUsuario' | 'nivelViajero' | 'fotoUrl'>;

export interface OpinionViajero {
    id: number;
    texto: string;
}

export interface ViajeHistorial {
    id: number;
    destino: string;
    fechaReserva: string; // ISO: YYYY-MM-DD
    montoTotal: number;
    estadoReserva: EstadoReserva;
}
