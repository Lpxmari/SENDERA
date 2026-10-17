import { AxiosError, AxiosHeaders, InternalAxiosRequestConfig } from 'axios';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import apiClient, { obtenerMensajeError } from './apiClient';
import { guardarSesion, leerSesion } from './sesion';

const SESION = {
    token: 'token-de-prueba',
    usuario: { id: 1, nombre: 'Ana', apellidos: 'Pérez', correo: 'ana@correo.com' },
};

/** Respuesta de error con el formato ApiError del backend. */
const errorHttp = (config: InternalAxiosRequestConfig, status: number, message: string): AxiosError =>
    new AxiosError(message, String(status), config, null, {
        status,
        statusText: '',
        headers: {},
        config,
        data: { timestamp: '', status, error: '', message, path: config.url ?? '' },
    });

describe('obtenerMensajeError', () => {
    const config = { headers: new AxiosHeaders() } as InternalAxiosRequestConfig;

    it('devuelve el mensaje seguro del backend', () => {
        expect(obtenerMensajeError(errorHttp(config, 409, 'El correo ya está registrado.'), 'por defecto')).toBe(
            'El correo ya está registrado.',
        );
    });

    it('avisa cuando no hay conexión con el servidor', () => {
        const sinRespuesta = new AxiosError('Network Error', 'ERR_NETWORK', config);

        expect(obtenerMensajeError(sinRespuesta, 'por defecto')).toBe(
            'No se pudo conectar con el servidor. Intenta de nuevo.',
        );
    });

    it('usa el mensaje por defecto cuando el error no viene de la API', () => {
        expect(obtenerMensajeError(new Error('interno'), 'por defecto')).toBe('por defecto');
        expect(obtenerMensajeError('texto', 'por defecto')).toBe('por defecto');
    });
});

describe('apiClient', () => {
    let peticion: InternalAxiosRequestConfig | undefined;
    const asignar = vi.fn();

    beforeEach(() => {
        peticion = undefined;
        vi.stubGlobal('location', { ...window.location, assign: asignar });
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('adjunta el JWT cuando hay sesión', async () => {
        guardarSesion(SESION);
        apiClient.defaults.adapter = async (config) => {
            peticion = config;
            return { data: [], status: 200, statusText: 'OK', headers: {}, config };
        };

        await apiClient.get('/destinos');

        expect(peticion?.headers.Authorization).toBe('Bearer token-de-prueba');
    });

    it('no envía Authorization sin sesión', async () => {
        apiClient.defaults.adapter = async (config) => {
            peticion = config;
            return { data: [], status: 200, statusText: 'OK', headers: {}, config };
        };

        await apiClient.get('/destinos');

        expect(peticion?.headers.Authorization).toBeUndefined();
    });

    it('cierra la sesión y redirige a /login ante un 401 con token vencido', async () => {
        guardarSesion(SESION);
        apiClient.defaults.adapter = async (config) => Promise.reject(errorHttp(config, 401, 'Sesión vencida'));

        await expect(apiClient.get('/reservas')).rejects.toBeInstanceOf(AxiosError);

        expect(leerSesion()).toBeNull();
        expect(asignar).toHaveBeenCalledWith('/login');
    });

    it('conserva la sesión si el 401 viene de una ruta /auth (credenciales incorrectas)', async () => {
        guardarSesion(SESION);
        apiClient.defaults.adapter = async (config) => Promise.reject(errorHttp(config, 401, 'Credenciales inválidas'));

        await expect(apiClient.post('/auth/login', {})).rejects.toBeInstanceOf(AxiosError);

        expect(leerSesion()).not.toBeNull();
        expect(asignar).not.toHaveBeenCalled();
    });
});
