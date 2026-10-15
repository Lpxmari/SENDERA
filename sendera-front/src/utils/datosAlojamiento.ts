// Datos de ejemplo tomados del mockup del detalle de alojamiento.
// El backend aún no expone estos campos (categoría, estrellas, calificación,
// reseñas, servicios, opiniones). Cuando existan en el DTO, reemplazar este archivo
// por los valores reales de `Destino`.

export interface Opinion {
    id: number;
    texto: string;
    autor: string;
    perfil: string;
}

export interface ServicioAlojamiento {
    id: string;
    etiqueta: string;
}

export const categoriaAlojamiento = 'Hotel boutique';
export const estrellasAlojamiento = 5;

export const calificacionAlojamiento = {
    puntaje: 9.2,
    etiqueta: 'Fantástico',
    totalResenas: 186,
};

export const eslogan = 'Una estancia tranquila entre historia y naturaleza';

export const serviciosAlojamiento: ServicioAlojamiento[] = [
    { id: 'piscina', etiqueta: 'Piscina' },
    { id: 'desayuno', etiqueta: 'Desayuno' },
    { id: 'wifi', etiqueta: 'Wi-Fi' },
    { id: 'estacionamiento', etiqueta: 'Estacionamiento' },
];

export const opinionesAlojamiento: Opinion[] = [
    { id: 1, texto: 'Una experiencia muy agradable', autor: 'María López', perfil: 'Adulto Mayor' },
    { id: 2, texto: 'Fue fácil organizar mi viaje', autor: 'Carlos Ramírez', perfil: 'Acompañante familiar' },
    { id: 3, texto: 'Encontré un lugar perfecto para descansar', autor: 'Ana Martínez', perfil: 'Adulto Mayor' },
];