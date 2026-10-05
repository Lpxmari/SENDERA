export interface Destino {
    id: number;
    nombre: string;
    descripcion: string;
    ubicacion: string;
    precioBase: number;
    fotosUrl: string[];
}

// Útil para enviar el formulario (el ID lo genera la BD)
export type CrearDestinoDto = Omit<Destino, 'id'>;