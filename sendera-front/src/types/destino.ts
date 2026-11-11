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

/** Página de resultados de la API (PaginaDestino en docs/openapi.yaml). `pagina` empieza en 0. */
export interface Pagina<T> {
    contenido: T[];
    pagina: number;
    tamano: number;
    totalElementos: number;
    totalPaginas: number;
}

export type PaginaDestinos = Pagina<Destino>;

// DTO para los parámetros de filtro en las búsquedas
export interface FiltroDestinoParams {
    q?: string;
    nombre?: string;
    ubicacion?: string;
    /** Página a pedir, desde 0 (por defecto 0). */
    page?: number;
    /** Destinos por página (por defecto 12; el backend limita a 50). */
    size?: number;
    precioMin?: number;
    precioMax?: number;
    dificultad?: DificultadEnum | '';
    accesibilidad?: boolean;
}