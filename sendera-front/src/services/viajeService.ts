import { GuardarViajeRequest, Viaje, ViajeResumen } from '../types/viaje';
import apiClient from './apiClient';

/** Solo devuelve los itinerarios de la persona con sesión iniciada (el backend toma su identidad del token). */
export const fetchViajes = async (): Promise<ViajeResumen[]> => {
    const { data } = await apiClient.get<ViajeResumen[]>('/viajes');
    return data;
};

/** 404 si el viaje no existe o es de otra persona: el backend no distingue ambos casos. */
export const fetchViaje = async (id: number): Promise<Viaje> => {
    const { data } = await apiClient.get<Viaje>(`/viajes/${id}`);
    return data;
};

export const crearViaje = async (cuerpo: GuardarViajeRequest): Promise<Viaje> => {
    const { data } = await apiClient.post<Viaje>('/viajes', cuerpo);
    return data; // 201 Created
};

/** Elimina el viaje y sus actividades colocadas (204). 404 si no existe o es de otra persona. */
export const eliminarViaje = async (id: number): Promise<void> => {
    await apiClient.delete(`/viajes/${id}`);
};

/** Reemplaza nombre, fechas y la lista completa de actividades. */
export const actualizarViaje = async (id: number, cuerpo: GuardarViajeRequest): Promise<Viaje> => {
    const { data } = await apiClient.put<Viaje>(`/viajes/${id}`, cuerpo);
    return data;
};
