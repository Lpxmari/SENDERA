import { beforeEach, describe, expect, it, vi } from 'vitest';
import apiClient from './apiClient';
import {
    cambiarActivoUsuario,
    cambiarEstadoReserva,
    cambiarRolUsuario,
    eliminarResenaAdmin,
    listarResenasAdmin,
    listarReservasAdmin,
    listarUsuariosAdmin,
} from './adminService';
import { actualizarDestino, crearDestino, eliminarDestino } from './destinoService';

vi.mock('./apiClient', () => ({
    default: { get: vi.fn(), patch: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
}));

const get = vi.mocked(apiClient.get);
const patch = vi.mocked(apiClient.patch);

describe('adminService', () => {
    beforeEach(() => {
        get.mockResolvedValue({ data: { contenido: [], pagina: 0, tamano: 20, total: 0 } });
        patch.mockResolvedValue({ data: { id: 2 } });
    });

    it('lista sin filtros enviando parámetros vacíos', async () => {
        await listarUsuariosAdmin();

        expect(get).toHaveBeenCalledWith('/admin/usuarios', { params: {} });
    });

    it('envía solo los filtros con valor, incluido activo=false', async () => {
        await listarUsuariosAdmin({ q: '  ana ', rol: 'ROLE_CLIENTE', activo: false, pagina: 2, tamano: 10 });

        expect(get).toHaveBeenCalledWith('/admin/usuarios', {
            params: { q: 'ana', rol: 'ROLE_CLIENTE', activo: false, pagina: 2, tamano: 10 },
        });
    });

    it('omite filtros vacíos', async () => {
        await listarUsuariosAdmin({ q: '   ', rol: '', activo: '' });

        expect(get).toHaveBeenCalledWith('/admin/usuarios', { params: {} });
    });

    it('cambia el rol y el estado con el cuerpo del contrato', async () => {
        await cambiarRolUsuario(2, 'ROLE_ADMIN');
        await cambiarActivoUsuario(2, false);

        expect(patch).toHaveBeenCalledWith('/admin/usuarios/2/rol', { rol: 'ROLE_ADMIN' });
        expect(patch).toHaveBeenCalledWith('/admin/usuarios/2/activo', { activo: false });
    });

    it('propaga el error sin silenciarlo', async () => {
        get.mockRejectedValue(new Error('fallo de red'));

        await expect(listarUsuariosAdmin()).rejects.toThrow('fallo de red');
    });
});

describe('destinoService (escritura de administrador)', () => {
    const DATOS = {
        nombre: 'Valle',
        descripcion: 'Paisaje',
        ubicacion: 'Salento',
        precioBase: 85000,
        dificultad: 'BAJA' as const,
        accesibilidad: true,
        fotosUrl: [],
    };

    it('crea, actualiza y elimina con las rutas del contrato', async () => {
        vi.mocked(apiClient.post).mockResolvedValue({ data: { id: 1, ...DATOS } });
        vi.mocked(apiClient.put).mockResolvedValue({ data: { id: 1, ...DATOS } });
        vi.mocked(apiClient.delete).mockResolvedValue({ data: undefined });

        await crearDestino(DATOS);
        await actualizarDestino(1, DATOS);
        await eliminarDestino(1);

        expect(apiClient.post).toHaveBeenCalledWith('/destinos', DATOS);
        expect(apiClient.put).toHaveBeenCalledWith('/destinos/1', DATOS);
        expect(apiClient.delete).toHaveBeenCalledWith('/destinos/1');
    });
});

describe('adminService: reservas y reseñas', () => {
    beforeEach(() => {
        get.mockResolvedValue({ data: { contenido: [], pagina: 0, tamano: 10, total: 0 } });
        patch.mockResolvedValue({ data: { id: 3 } });
    });

    it('lista reservas enviando solo los filtros con valor', async () => {
        await listarReservasAdmin({
            estado: 'CONFIRMADA',
            q: ' ana ',
            desde: '2026-10-01',
            hasta: '',
            pagina: 1,
            tamano: 10,
        });

        expect(get).toHaveBeenCalledWith('/admin/reservas', {
            params: { estado: 'CONFIRMADA', q: 'ana', desde: '2026-10-01', pagina: 1, tamano: 10 },
        });
    });

    it('lista reservas sin filtros con parámetros vacíos', async () => {
        await listarReservasAdmin();

        expect(get).toHaveBeenCalledWith('/admin/reservas', { params: {} });
    });

    it('cambia el estado de una reserva con el cuerpo del contrato', async () => {
        await cambiarEstadoReserva(3, 'CANCELADA');

        expect(patch).toHaveBeenCalledWith('/admin/reservas/3/estado', { estado: 'CANCELADA' });
    });

    it('lista reseñas con destino, calificación máxima y texto', async () => {
        await listarResenasAdmin({ destinoId: 5, calificacionMax: 2, q: 'sucio', pagina: 0, tamano: 10 });

        expect(get).toHaveBeenCalledWith('/admin/resenas', {
            params: { destinoId: 5, calificacionMax: 2, q: 'sucio', pagina: 0, tamano: 10 },
        });
    });

    it('omite los filtros de reseñas vacíos', async () => {
        await listarResenasAdmin({ destinoId: '', calificacionMax: '', q: '  ' });

        expect(get).toHaveBeenCalledWith('/admin/resenas', { params: {} });
    });

    it('elimina una reseña', async () => {
        vi.mocked(apiClient.delete).mockResolvedValue({ data: undefined });

        await eliminarResenaAdmin(9);

        expect(apiClient.delete).toHaveBeenCalledWith('/admin/resenas/9');
    });
});
