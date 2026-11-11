import { beforeEach, describe, expect, it, vi } from 'vitest';
import apiClient from './apiClient';
import { listarDestinos, listarTodosLosDestinos, obtenerDestinoPorId } from './destinoService';

vi.mock('./apiClient', () => ({ default: { get: vi.fn() } }));

const get = vi.mocked(apiClient.get);

describe('destinoService', () => {
    beforeEach(() => {
        get.mockReset();
        get.mockResolvedValue({ data: { contenido: [], pagina: 0, tamano: 12, totalElementos: 0, totalPaginas: 0 } });
    });

    it('devuelve la respuesta paginada tal como llega', async () => {
        const respuesta = { contenido: [{ id: 1 }], pagina: 2, tamano: 12, totalElementos: 30, totalPaginas: 3 };
        get.mockResolvedValue({ data: respuesta });

        await expect(listarDestinos({ page: 2 })).resolves.toEqual(respuesta);
    });

    it('envía page y size, también page=0', async () => {
        await listarDestinos({ page: 0, size: 6 });

        expect(get).toHaveBeenCalledWith('/destinos', { params: { page: 0, size: 6 } });
    });

    it('envía nombre y ubicacion junto con los demás filtros', async () => {
        await listarDestinos({ nombre: 'valle', ubicacion: 'salento', accesibilidad: true, page: 1 });

        expect(get).toHaveBeenCalledWith('/destinos', {
            params: { nombre: 'valle', ubicacion: 'salento', accesibilidad: true, page: 1 },
        });
    });

    it('listarTodosLosDestinos recorre todas las páginas de 50', async () => {
        get.mockImplementation(async (_url, config) => {
            const page = (config as { params: { page: number } }).params.page;
            return { data: { contenido: [{ id: page * 2 + 1 }, { id: page * 2 + 2 }], pagina: page, tamano: 50, totalElementos: 6, totalPaginas: 3 } };
        });

        const todos = await listarTodosLosDestinos();

        expect(todos.map((d) => d.id)).toEqual([1, 2, 3, 4, 5, 6]);
        expect(get).toHaveBeenCalledTimes(3);
        expect(get).toHaveBeenNthCalledWith(1, '/destinos', { params: { page: 0, size: 50 } });
        expect(get).toHaveBeenNthCalledWith(3, '/destinos', { params: { page: 2, size: 50 } });
    });

    it('listarTodosLosDestinos con una sola página hace una sola consulta', async () => {
        await listarTodosLosDestinos();

        expect(get).toHaveBeenCalledTimes(1);
    });

    it('lista sin filtros enviando parámetros vacíos', async () => {
        await listarDestinos();

        expect(get).toHaveBeenCalledWith('/destinos', { params: {} });
    });

    it('envía solo los filtros definidos, incluida accesibilidad=false', async () => {
        await listarDestinos({ q: 'cocora', precioMax: 100000, dificultad: 'BAJA', accesibilidad: false });

        expect(get).toHaveBeenCalledWith('/destinos', {
            params: { q: 'cocora', precioMax: 100000, dificultad: 'BAJA', accesibilidad: false },
        });
    });

    it('omite filtros vacíos', async () => {
        await listarDestinos({ q: '', dificultad: '' });

        expect(get).toHaveBeenCalledWith('/destinos', { params: {} });
    });

    it('consulta el detalle por id', async () => {
        get.mockResolvedValue({ data: { id: 7 } });

        await expect(obtenerDestinoPorId(7)).resolves.toEqual({ id: 7 });
        expect(get).toHaveBeenCalledWith('/destinos/7');
    });

    it('propaga el error sin silenciarlo', async () => {
        get.mockRejectedValue(new Error('fallo de red'));

        await expect(listarDestinos()).rejects.toThrow('fallo de red');
    });
});
