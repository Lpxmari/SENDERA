import { AuthResponse } from '../types/auth';

const CLAVE_SESION = 'sendera.sesion';

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
