import { CrearResena, Resena } from '../types/resena';
import apiClient from './apiClient';

export const listarResenas = async (destinoId: number): Promise<Resena[]> => {
    const { data } = await apiClient.get<Resena[]>(`/destinos/${destinoId}/resenas`);
    return data;
};

export const crearResena = async (
    destinoId: number,
    datos: CrearResena,
    imagen: File | null,
): Promise<Resena> => {
    const formulario = new FormData();
    formulario.append('comentario', datos.comentario);
    formulario.append('calificacion', String(datos.calificacion));
    if (imagen) formulario.append('imagen', imagen);

    const { data } = await apiClient.post<Resena>(`/destinos/${destinoId}/resenas`, formulario);
    return data;
};

export const obtenerUrlImagenResena = (ruta: string): string => apiClient.getUri({ url: ruta });
