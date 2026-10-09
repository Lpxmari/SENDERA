import axios, { AxiosError } from 'axios';
import {
    AuthResponse,
    CambiarContrasenaRequest,
    LoginRequest,
    RecuperarContrasenaRequest,
    RegistroRequest,
} from '../types/auth';
import { ApiError } from '../types/api';

const API_URL = 'http://localhost:8080/api/v1/auth';
const CLAVE_SESION = 'sendera.sesion';

/** Devuelve el mensaje de error del backend o uno por defecto. */
export const obtenerMensajeError = (error: unknown, porDefecto: string): string => {
    const err = error as AxiosError<ApiError>;
    if (err.response?.data?.message) return err.response.data.message;
    if (err.request && !err.response) return 'No se pudo conectar con el servidor. Intenta de nuevo.';
    return porDefecto;
};

export const guardarSesion = (sesion: AuthResponse): void => {
    localStorage.setItem(CLAVE_SESION, JSON.stringify(sesion));
};

export const leerSesion = (): AuthResponse | null => {
    try {
        const raw = localStorage.getItem(CLAVE_SESION);
        return raw ? (JSON.parse(raw) as AuthResponse) : null;
    } catch {
        return null;
    }
};

export const borrarSesion = (): void => localStorage.removeItem(CLAVE_SESION);

export const iniciarSesion = async (datos: LoginRequest): Promise<AuthResponse> => {
    const { data } = await axios.post<AuthResponse>(`${API_URL}/login`, datos);
    return data;
};

export const registrarUsuario = async (datos: RegistroRequest): Promise<AuthResponse> => {
    const { data } = await axios.post<AuthResponse>(`${API_URL}/registro`, datos);
    return data;
};

export const solicitarRecuperacion = async (datos: RecuperarContrasenaRequest): Promise<void> => {
    await axios.post(`${API_URL}/recuperar-contrasena`, datos);
};

export const cambiarContrasena = async (datos: CambiarContrasenaRequest): Promise<void> => {
    await axios.post(`${API_URL}/cambiar-contrasena`, datos);
};

// Adjunta el token JWT a todas las peticiones cuando hay sesión activa.
axios.interceptors.request.use((config) => {
    const sesion = leerSesion();
    if (sesion?.token) config.headers.Authorization = `Bearer ${sesion.token}`;
    return config;
});
