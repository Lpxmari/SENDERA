export type CategoriaViaje =
    | 'Recorridos Históricos'
    | 'Comida y Cocina'
    | 'Relajación Costera'
    | 'Avistamiento de Fauna';

export interface SugerenciaViaje {
    id: number;
    nombre: string;
    tipo: string;
    lugar: string;
    categoria: CategoriaViaje;
    imagenUrl?: string;
}