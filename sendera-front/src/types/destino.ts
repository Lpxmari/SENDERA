export type DificultadEnum = 'BAJA' | 'MODERADA' | 'ALTA';

export interface Destino {
    id: number;
    nombre: string;
    descripcion: string;
    ubicacion: string;
    precioBase: number;
    dificultad: DificultadEnum;   // <-- AÑADIDO
    accesibilidad: boolean;       // <-- AÑADIDO
    fotosUrl: string[];
}

// Útil para enviar el formulario (el ID lo genera la BD)
// Hereda automáticamente 'dificultad' y 'accesibilidad'
export type CrearDestinoDto = Omit<Destino, 'id'>;

// DTO para los parámetros de filtro en las búsquedas
export interface FiltroDestinoParams {
    q?: string;
    precioMin?: number;
    precioMax?: number;
    duracionMin?: number;
    duracionMax?: number;
    dificultad?: DificultadEnum | '';
    accesibilidad?: boolean;
}