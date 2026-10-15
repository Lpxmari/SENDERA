import axios, { AxiosError } from 'axios';
import { ApiError } from '../types/api';
import { borrarSesion, leerSesion } from './sesion';

/** En desarrollo, Vite redirige /api al backend (ver vite.config.js). En producción define VITE_API_URL. */
const BASE_URL: string = import.meta.env.VITE_API_URL ?? '/api/v1';

const apiClient = axios.create({ baseURL: BASE_URL });

// Adjunta el JWT a todas las peticiones cuando hay sesión activa.
apiClient.interceptors.request.use((config) => {
    const sesion = leerSesion();
    if (sesion?.token) config.headers.Authorization = `Bearer ${sesion.token}`;
    return config;
});

// Un 401 con sesión guardada significa que el token venció o es inválido: se cierra la sesión.
// Las rutas /auth/* quedan fuera porque un 401 en el login es "credenciales incorrectas".
apiClient.interceptors.response.use(
    (respuesta) => respuesta,
    (error: unknown) => {
        if (
            axios.isAxiosError(error) &&
            error.response?.status === 401 &&
            leerSesion() !== null &&
            !error.config?.url?.startsWith('/auth/')
        ) {
            borrarSesion();
            window.location.assign('/login');
        }
        return Promise.reject(error);
    },
);

/** Devuelve el mensaje seguro del backend (ApiError) o uno por defecto. No registra datos en consola. */
export const obtenerMensajeError = (error: unknown, porDefecto: string): string => {
    if (axios.isAxiosError<ApiError>(error)) {
        const mensaje = (error as AxiosError<ApiError>).response?.data?.message;
        if (mensaje) return mensaje;
        if (!error.response) return 'No se pudo conectar con el servidor. Intenta de nuevo.';
    }
    return porDefecto;
};

export default apiClient;
