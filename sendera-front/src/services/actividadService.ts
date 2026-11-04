import { Actividad, DuracionActividad } from '../types/viaje';
import apiClient from './apiClient';

/**
 * Catálogo público de actividades; se puede filtrar por destino y por rango de duración (atributo de la actividad).
 * Los errores se propagan a la capa de UI.
 */
export const fetchActividades = async (destinoId?: number, duracion?: DuracionActividad): Promise<Actividad[]> => {
    const params: Record<string, number | string> = {};
    if (destinoId !== undefined) params.destinoId = destinoId;
    if (duracion !== undefined) params.duracion = duracion;
    const { data } = await apiClient.get<Actividad[]>('/actividades', { params });
    return data;
};
