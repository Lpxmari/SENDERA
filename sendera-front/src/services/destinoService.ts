import { CrearDestinoDto, Destino, FiltroDestinoParams } from '../types/destino';
import apiClient from './apiClient';

/** Los errores se propagan sin silenciarlos; la capa de UI decide cómo mostrarlos (ver obtenerMensajeError). */
export const listarDestinos = async (filtros?: FiltroDestinoParams): Promise<Destino[]> => {
    const params: Record<string, string | number | boolean> = {};
    if (filtros?.q) params.q = filtros.q;
    if (filtros?.precioMin !== undefined) params.precioMin = filtros.precioMin;
    if (filtros?.precioMax !== undefined) params.precioMax = filtros.precioMax;
    if (filtros?.duracionMin !== undefined) params.duracionMin = filtros.duracionMin;
    if (filtros?.duracionMax !== undefined) params.duracionMax = filtros.duracionMax;
    if (filtros?.dificultad) params.dificultad = filtros.dificultad;
    if (filtros?.accesibilidad !== undefined && filtros.accesibilidad !== null) {
        params.accesibilidad = filtros.accesibilidad;
    }

    const { data } = await apiClient.get<Destino[]>('/destinos', { params });
    return data;
};

export const obtenerDestinoPorId = async (id: number): Promise<Destino> => {
    const { data } = await apiClient.get<Destino>(`/destinos/${id}`);
    return data;
};

/** Solo administradores (el backend responde 403 a cualquier otro rol). */
export const crearDestino = async (datos: CrearDestinoDto): Promise<Destino> => {
    const { data } = await apiClient.post<Destino>('/destinos', datos);
    return data;
};

export const actualizarDestino = async (id: number, datos: CrearDestinoDto): Promise<Destino> => {
    const { data } = await apiClient.put<Destino>(`/destinos/${id}`, datos);
    return data;
};

/** 409 si el destino tiene itinerarios asociados. */
export const eliminarDestino = async (id: number): Promise<void> => {
    await apiClient.delete(`/destinos/${id}`);
};

// Alias para mantener compatibilidad con los componentes que usan los nombres anteriores
export const fetchDestinos = listarDestinos;
export const getDestinoPorId = obtenerDestinoPorId;
