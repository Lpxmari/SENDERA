import { beforeEach, describe, expect, it, vi } from 'vitest';
import apiClient from './apiClient';
import { actualizarCuenta, cambiarContrasena, solicitarRecuperacion, verificarCodigo } from './authService';

vi.mock('./apiClient', () => ({ default: { put: vi.fn(), post: vi.fn() }, obtenerMensajeError: vi.fn() }));

describe('authService', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('actualiza la cuenta enviando el cuerpo del contrato a PUT /auth/me', async () => {
        vi.mocked(apiClient.put).mockResolvedValue({
            data: { token: 'nuevo', usuario: { id: 1, nombre: 'Ana', apellidos: 'P', correo: 'a@b.com' } },
        });

        const cuerpo = { nombre: 'Ana', apellidos: 'P', correo: 'a@b.com', contrasenaActual: 'clave1234' };
        const sesion = await actualizarCuenta(cuerpo);

        expect(apiClient.put).toHaveBeenCalledWith('/auth/me', cuerpo);
        expect(sesion.token).toBe('nuevo');
    });

    it('solicita el código de recuperación con POST /auth/recuperar-contrasena', async () => {
        vi.mocked(apiClient.post).mockResolvedValue({ data: null });

        await solicitarRecuperacion({ correo: 'maria@correo.com' });

        expect(apiClient.post).toHaveBeenCalledWith('/auth/recuperar-contrasena', { correo: 'maria@correo.com' });
    });

    it('verifica el código con POST /auth/verificar-codigo', async () => {
        vi.mocked(apiClient.post).mockResolvedValue({ data: null });

        await verificarCodigo({ correo: 'maria@correo.com', codigo: '123456' });

        expect(apiClient.post).toHaveBeenCalledWith('/auth/verificar-codigo', { correo: 'maria@correo.com', codigo: '123456' });
    });

    it('cambia la contraseña con POST /auth/cambiar-contrasena', async () => {
        vi.mocked(apiClient.post).mockResolvedValue({ data: null });

        await cambiarContrasena({ correo: 'maria@correo.com', codigo: '123456', nuevaContrasena: 'nueva1234' });

        expect(apiClient.post).toHaveBeenCalledWith('/auth/cambiar-contrasena', {
            correo: 'maria@correo.com',
            codigo: '123456',
            nuevaContrasena: 'nueva1234',
        });
    });

    it('propaga el error de actualizarCuenta sin silenciarlo', async () => {
        vi.mocked(apiClient.put).mockRejectedValue(new Error('fallo de red'));

        await expect(actualizarCuenta({ nombre: 'A', apellidos: 'B', correo: 'a@b.com' })).rejects.toThrow('fallo de red');
    });
});
