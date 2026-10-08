import { Destino } from '../types/destino';
import { calificacionAlojamiento } from './datosAlojamiento';

// Datos de ejemplo del mockup de resultados. El backend aún no entrega estrellas,
// servicios, insignias, calificación ni precio anterior; se generan aquí de forma
// determinista a partir del id. Reemplazar por los valores reales cuando existan en el DTO.

export const serviciosFiltro = ['Desayuno incluido', 'Piscina', 'Estacionamiento', 'Admite mascotas'] as const;
export const estrellasFiltro = [5, 4, 3] as const;

export interface RangoPresupuesto {
    id: string;
    etiqueta: string;
    min: number;
    max: number;
}

export const rangosPresupuesto: RangoPresupuesto[] = [
    { id: 'bajo', etiqueta: 'Menos de COP 450.000', min: 0, max: 449999 },
    { id: 'medio', etiqueta: 'COP 450.000–650.000', min: 450000, max: 650000 },
    { id: 'alto', etiqueta: 'Más de COP 650.000', min: 650001, max: Infinity },
];

export const opcionesOrden = [
    { valor: 'recomendados', etiqueta: 'Recomendados' },
    { valor: 'precio-asc', etiqueta: 'Precio: menor a mayor' },
    { valor: 'precio-desc', etiqueta: 'Precio: mayor a menor' },
] as const;

export type Orden = (typeof opcionesOrden)[number]['valor'];

const insignias = ['Desayuno incluido', 'Cancelación gratis', 'Oferta de temporada'] as const;

export interface DatosResultado {
    estrellas: number;
    servicios: string[];
    insignia: string;
    precioAnterior: number;
    puntaje: number;
    etiquetaPuntaje: string;
    resenas: number;
}

export const datosResultado = (destino: Destino): DatosResultado => {
    const insignia = insignias[destino.id % insignias.length];
    const servicios: string[] = serviciosFiltro.filter((_, indice) => (destino.id + indice) % 3 !== 1);
    if (insignia === 'Desayuno incluido' && !servicios.includes(insignia)) servicios.push(insignia);

    return {
        estrellas: 3 + (destino.id % 3),
        servicios,
        insignia,
        precioAnterior: Math.round((destino.precioBase * 1.22) / 100) * 100,
        puntaje: calificacionAlojamiento.puntaje,
        etiquetaPuntaje: calificacionAlojamiento.etiqueta,
        resenas: calificacionAlojamiento.totalResenas,
    };
};

const MS_POR_DIA = 1000 * 60 * 60 * 24;

export const calcularNoches = (llegada: string, salida: string): number => {
    if (!llegada || !salida) return 0;
    const diferencia = (new Date(salida).getTime() - new Date(llegada).getTime()) / MS_POR_DIA;
    return Number.isFinite(diferencia) && diferencia > 0 ? Math.round(diferencia) : 0;
};