import { CrearReservaRequest, Reserva } from '../types/reserva';
import apiClient from './apiClient';

/** Reservas del usuario autenticado (el backend filtra por el JWT). */
export const listarMisReservas = async (): Promise<Reserva[]> => {
    const { data } = await apiClient.get<Reserva[]>('/reservas');
    return data;
};

/** El monto y el estado los calcula el backend; aquí solo se envía lo que el contrato permite. */
export const crearReserva = async (reservaData: CrearReservaRequest): Promise<Reserva> => {
    const { data } = await apiClient.post<Reserva>('/reservas', reservaData);
    return data; // 201 Created
};
