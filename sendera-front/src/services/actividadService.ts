import { Actividad } from '../types/viaje';
import apiClient from './apiClient';

/** Catálogo público de actividades; se puede filtrar por destino. Los errores se propagan a la capa de UI. */
export const fetchActividades = async (destinoId?: number): Promise<Actividad[]> => {
    const { data } = await apiClient.get<Actividad[]>('/actividades', {
        params: destinoId !== undefined ? { destinoId } : undefined,
    });
    return data;
};
