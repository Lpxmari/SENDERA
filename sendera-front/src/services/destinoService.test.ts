import { beforeEach, describe, expect, it, vi } from 'vitest';
import apiClient from './apiClient';
import { listarDestinos, obtenerDestinoPorId } from './destinoService';

vi.mock('./apiClient', () => ({ default: { get: vi.fn() } }));

const get = vi.mocked(apiClient.get);

describe('destinoService', () => {
    beforeEach(() => {
        get.mockResolvedValue({ data: [] });
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
