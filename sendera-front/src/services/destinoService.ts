import axios, { AxiosError } from 'axios';
import { Destino, FiltroDestinoParams } from '../types/destino';
import { ApiError } from '../types/api';

const API_URL = 'http://localhost:8080/destinos'; // Asegúrate de que coincida con el RequestMapping del Backend

export const listarDestinos = async (filtros?: FiltroDestinoParams): Promise<Destino[]> => {
    try {
        const queryParams = new URLSearchParams();

        if (filtros?.q) queryParams.append('q', filtros.q);
        if (filtros?.precioMin !== undefined) queryParams.append('precioMin', filtros.precioMin.toString());
        if (filtros?.precioMax !== undefined) queryParams.append('precioMax', filtros.precioMax.toString());
        if (filtros?.duracionMin !== undefined) queryParams.append('duracionMin', filtros.duracionMin.toString());
        if (filtros?.duracionMax !== undefined) queryParams.append('duracionMax', filtros.duracionMax.toString());
        if (filtros?.dificultad) queryParams.append('dificultad', filtros.dificultad);
        if (filtros?.accesibilidad !== undefined && filtros?.accesibilidad !== null) {
            queryParams.append('accesibilidad', filtros.accesibilidad.toString());
        }

        const url = queryParams.toString() ? `${API_URL}?${queryParams.toString()}` : API_URL;
        const response = await axios.get<Destino[]>(url);
        return response.data;
    } catch (error) {
        const err = error as AxiosError<ApiError>;
        if (err.response) {
            console.error(`Error ${err.response.data.status}: ${err.response.data.message}`);
        }
        throw error;
    }
};

export const obtenerDestinoPorId = async (id: number): Promise<Destino> => {
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

// Aliases para mantener compatibilidad si otros componentes usan los nombres viejos
export const fetchDestinos = listarDestinos;
export const getDestinoPorId = obtenerDestinoPorId;