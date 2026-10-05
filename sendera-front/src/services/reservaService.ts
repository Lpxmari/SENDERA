import axios, { AxiosError } from 'axios';
import { Reserva } from '../types/reserva';
import { ApiError } from '../types/api';

const API_URL = 'http://localhost:8080/api/v1/reservas';

export const crearReserva = async (reservaData: Omit<Reserva, 'id'>): Promise<Reserva> => {
    try {
        const response = await axios.post<Reserva>(API_URL, reservaData);
        return response.data; // retorna estado 201 Created
    } catch (error) {
        const err = error as AxiosError<ApiError>;
        if (err.response) {
            console.error(`Error ${err.response.data.status}: ${err.response.data.message}`);
        }
        throw error;
    }
};