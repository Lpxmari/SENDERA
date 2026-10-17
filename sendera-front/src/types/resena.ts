export interface Resena {
    id: number;
    destinoId: number;
    comentario: string;
    calificacion: number;
    autor: string;
    creadaEn: string;
    imagenUrl: string | null;
}

export interface CrearResena {
    comentario: string;
    calificacion: number;
}
