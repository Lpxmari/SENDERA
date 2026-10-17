import { CrearReservaRequest, Reserva } from '../types/reserva';
import apiClient from './apiClient';

/** El monto y el estado los calcula el backend; aquí solo se envía lo que el contrato permite. */
export const crearReserva = async (reservaData: CrearReservaRequest): Promise<Reserva> => {
    const { data } = await apiClient.post<Reserva>('/reservas', reservaData);
    return data; // 201 Created
};
