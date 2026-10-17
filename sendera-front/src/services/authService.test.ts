import { beforeEach, describe, expect, it, vi } from 'vitest';
import apiClient from './apiClient';
import { actualizarCuenta } from './authService';

vi.mock('./apiClient', () => ({ default: { put: vi.fn() }, obtenerMensajeError: vi.fn() }));

describe('authService.actualizarCuenta', () => {
    beforeEach(() => {
        vi.mocked(apiClient.put).mockResolvedValue({
            data: { token: 'nuevo', usuario: { id: 1, nombre: 'Ana', apellidos: 'P', correo: 'a@b.com' } },
        });
    });

    it('envía el cuerpo del contrato a PUT /auth/me y devuelve la sesión nueva', async () => {
        const cuerpo = { nombre: 'Ana', apellidos: 'P', correo: 'a@b.com', contrasenaActual: 'clave1234' };

        const sesion = await actualizarCuenta(cuerpo);

        expect(apiClient.put).toHaveBeenCalledWith('/auth/me', cuerpo);
        expect(sesion.token).toBe('nuevo');
    });

    it('propaga el error sin silenciarlo', async () => {
        vi.mocked(apiClient.put).mockRejectedValue(new Error('fallo de red'));

        await expect(actualizarCuenta({ nombre: 'A', apellidos: 'B', correo: 'a@b.com' })).rejects.toThrow(
            'fallo de red',
        );
    });
});
