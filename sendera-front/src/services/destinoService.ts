import axios, { AxiosError } from 'axios';
import { Destino } from '../types/destino';
import { ApiError } from '../types/api';

// Ajusta la URL base si el backend definió otro prefijo
const API_URL = 'http://localhost:8080/api/v1/destinos';

export const fetchDestinos = async (): Promise<Destino[]> => {
    try {
        const response = await axios.get<Destino[]>(API_URL);
        return response.data;
    } catch (error) {
        const err = error as AxiosError<ApiError>;
        if (err.response) {
            console.error(`Error ${err.response.data.status}: ${err.response.data.message}`);
        }
        throw error;
    }
};

export const getDestinoPorId = async (id: number): Promise<Destino> => {
    try {
        const response = await axios.get<Destino>(`${API_URL}/${id}`);
        return response.data;
    } catch (error) {
        const err = error as AxiosError<ApiError>;
        if (err.response) {
            console.error(`Error ${err.response.data.status}: ${err.response.data.message}`);
        }
        throw error;
    }
};