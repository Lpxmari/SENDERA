import {
    ActualizarCuentaRequest,
    AuthResponse,
    CambiarContrasenaRequest,
    LoginRequest,
    RecuperarContrasenaRequest,
    RegistroRequest,
} from '../types/auth';
import apiClient from './apiClient';

export { obtenerMensajeError } from './apiClient';
export { borrarSesion, guardarSesion, leerSesion } from './sesion';

export const iniciarSesion = async (datos: LoginRequest): Promise<AuthResponse> => {
    const { data } = await apiClient.post<AuthResponse>('/auth/login', datos);
    return data;
};

export const registrarUsuario = async (datos: RegistroRequest): Promise<AuthResponse> => {
    const { data } = await apiClient.post<AuthResponse>('/auth/registro', datos);
    return data;
};

/** Devuelve una sesión nueva: el correo es el sujeto del JWT, así que el token anterior deja de servir si cambió. */
export const actualizarCuenta = async (datos: ActualizarCuentaRequest): Promise<AuthResponse> => {
    const { data } = await apiClient.put<AuthResponse>('/auth/me', datos);
    return data;
};

export const solicitarRecuperacion = async (datos: RecuperarContrasenaRequest): Promise<void> => {
    await apiClient.post('/auth/recuperar-contrasena', datos);
};

export const cambiarContrasena = async (datos: CambiarContrasenaRequest): Promise<void> => {
    await apiClient.post('/auth/cambiar-contrasena', datos);
};
