// Datos visuales del mockup del detalle de alojamiento. Las reseñas y su
// calificación se obtienen del backend.

export interface ServicioAlojamiento {
    id: string;
    etiqueta: string;
}

export const categoriaAlojamiento = 'Hotel boutique';
export const estrellasAlojamiento = 5;

export const eslogan = 'Una estancia tranquila entre historia y naturaleza';

export const serviciosAlojamiento: ServicioAlojamiento[] = [
    { id: 'piscina', etiqueta: 'Piscina' },
    { id: 'desayuno', etiqueta: 'Desayuno' },
    { id: 'wifi', etiqueta: 'Wi-Fi' },
    { id: 'estacionamiento', etiqueta: 'Estacionamiento' },
];
