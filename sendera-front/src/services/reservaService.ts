import { CrearReservaRequest, PreferenciaPago, Reserva } from '../types/reserva';
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

/** Crea el pago de Mercado Pago de una reserva propia; el monto lo fija el backend. */
export const iniciarPago = async (reservaId: number): Promise<PreferenciaPago> => {
    const { data } = await apiClient.post<PreferenciaPago>(`/reservas/${reservaId}/pago`);
    return data;
};

/** El backend consulta el pago en Mercado Pago (nunca se confía en la URL de retorno) y devuelve la reserva. */
export const verificarPago = async (reservaId: number): Promise<Reserva> => {
    const { data } = await apiClient.post<Reserva>(`/reservas/${reservaId}/pago/verificar`);
    return data;
};
