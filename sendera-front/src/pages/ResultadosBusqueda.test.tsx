import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { fetchDestinos } from '../services/destinoService';
import { Destino, PaginaDestinos } from '../types/destino';
import ResultadosBusqueda from './ResultadosBusqueda';

vi.mock('../services/destinoService', () => ({ fetchDestinos: vi.fn() }));

const destino = (id: number, extras: Partial<Destino> = {}): Destino => ({
    id,
    nombre: `Alojamiento ${id}`,
    descripcion: 'Descripción',
    ubicacion: 'Salento, Quindío',
    precioBase: 300000,
    dificultad: 'BAJA',
    accesibilidad: true,
    fotosUrl: [],
    ...extras,
});

/** Simula un backend con `total` destinos y páginas de 12. */
const servidor = (total: number) => async (filtros?: { page?: number; size?: number }): Promise<PaginaDestinos> => {
    const pagina = filtros?.page ?? 0;
    const tamano = filtros?.size ?? 12;
    const ids = Array.from({ length: total }, (_, i) => i + 1).slice(pagina * tamano, (pagina + 1) * tamano);
    return {
        contenido: ids.map((id) => destino(id)),
        pagina,
        tamano,
        totalElementos: total,
        totalPaginas: Math.ceil(total / tamano),
    };
};

const renderizar = (ruta = '/resultados') =>
    render(
        <MemoryRouter initialEntries={[ruta]}>
            <ResultadosBusqueda />
        </MemoryRouter>,
    );

describe('ResultadosBusqueda: paginación (OB-01)', () => {
    beforeEach(() => {
        vi.mocked(fetchDestinos).mockReset();
        vi.mocked(fetchDestinos).mockImplementation(servidor(30));
    });

    afterEach(() => {
        delete (Element.prototype as { scrollIntoView?: unknown }).scrollIntoView;
    });

    it('pide la página 0 de 12 y muestra «Página 1 de 3» con el total en el título', async () => {
        renderizar();

        const paginacion = await screen.findByRole('navigation', { name: 'Paginación de resultados' });
        expect(fetchDestinos).toHaveBeenCalledWith({ page: 0, size: 12 });
        expect(within(paginacion).getByText('Página 1 de 3')).toBeInTheDocument();
        expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Todos los destinos: 30 alojamientos');
        expect(screen.getAllByRole('heading', { level: 3 })).toHaveLength(12);
    });

    it('en la primera página «Anterior» está deshabilitado; los botones tienen texto y nombre claro', async () => {
        renderizar();

        const anterior = await screen.findByRole('button', { name: /página anterior/ });
        const siguiente = screen.getByRole('button', { name: /página siguiente/ });
        expect(anterior).toBeDisabled();
        expect(anterior).toHaveTextContent('Anterior');
        expect(siguiente).toBeEnabled();
        expect(siguiente).toHaveTextContent('Siguiente');
        expect(siguiente).toHaveAccessibleName('Ir a la página siguiente, la 2 de 3');
    });

    it('«Siguiente» pide la página 1, muestra sus destinos y sube al inicio de los resultados', async () => {
        const scrollIntoView = vi.fn();
        Element.prototype.scrollIntoView = scrollIntoView;
        renderizar();

        fireEvent.click(await screen.findByRole('button', { name: /página siguiente/ }));

        expect(await screen.findByRole('heading', { level: 3, name: 'Alojamiento 13' })).toBeInTheDocument();
        expect(fetchDestinos).toHaveBeenLastCalledWith({ page: 1, size: 12 });
        expect(screen.queryByRole('heading', { level: 3, name: 'Alojamiento 1' })).not.toBeInTheDocument();
        expect(screen.getByText('Página 2 de 3')).toBeInTheDocument();
        expect(scrollIntoView).toHaveBeenCalledTimes(1);
        expect((scrollIntoView.mock.contexts[0] as HTMLElement).id).toBe('titulo-resultados');
        expect(screen.getByRole('heading', { level: 1 })).toHaveFocus();
    });

    it('en la última página «Siguiente» queda deshabilitado y «Anterior» vuelve atrás', async () => {
        renderizar();
        fireEvent.click(await screen.findByRole('button', { name: /página siguiente/ }));
        await screen.findByText('Página 2 de 3');
        fireEvent.click(screen.getByRole('button', { name: /página siguiente/ }));

        await screen.findByText('Página 3 de 3');
        expect(screen.getByRole('button', { name: /página siguiente/ })).toBeDisabled();
        expect(screen.getAllByRole('heading', { level: 3 })).toHaveLength(6);

        fireEvent.click(screen.getByRole('button', { name: /página anterior/ }));
        await screen.findByText('Página 2 de 3');
        expect(fetchDestinos).toHaveBeenLastCalledWith({ page: 1, size: 12 });
    });

    it('conserva los filtros al cambiar de página', async () => {
        renderizar('/resultados?destino=Salento');
        await screen.findByText('Página 1 de 3');

        fireEvent.click(screen.getByLabelText('Solo accesibles'));
        await waitFor(() =>
            expect(fetchDestinos).toHaveBeenLastCalledWith({ q: 'Salento', accesibilidad: true, page: 0, size: 12 }),
        );
        fireEvent.click(await screen.findByRole('button', { name: /página siguiente/ }));

        await waitFor(() =>
            expect(fetchDestinos).toHaveBeenLastCalledWith({ q: 'Salento', accesibilidad: true, page: 1, size: 12 }),
        );
        expect(screen.getByLabelText('Solo accesibles')).toBeChecked();
    });

    it('al cambiar un filtro vuelve a la primera página', async () => {
        renderizar();
        fireEvent.click(await screen.findByRole('button', { name: /página siguiente/ }));
        await screen.findByText('Página 2 de 3');

        fireEvent.click(screen.getByLabelText('Baja'));

        await waitFor(() => expect(fetchDestinos).toHaveBeenLastCalledWith({ dificultad: 'BAJA', page: 0, size: 12 }));
        expect(await screen.findByText('Página 1 de 3')).toBeInTheDocument();
    });

    it('envía el rango de precio al backend', async () => {
        renderizar();
        await screen.findByText('Página 1 de 3');

        fireEvent.click(screen.getByLabelText('COP 450.000–650.000'));

        await waitFor(() =>
            expect(fetchDestinos).toHaveBeenLastCalledWith({ precioMin: 450000, precioMax: 650000, page: 0, size: 12 }),
        );
    });

    it('con una sola página no muestra los controles de paginación', async () => {
        vi.mocked(fetchDestinos).mockImplementation(servidor(5));
        renderizar();

        await screen.findByRole('heading', { level: 3, name: 'Alojamiento 5' });
        expect(screen.queryByRole('navigation', { name: 'Paginación de resultados' })).not.toBeInTheDocument();
    });

    it('sin resultados muestra el mensaje de lista vacía', async () => {
        vi.mocked(fetchDestinos).mockImplementation(servidor(0));
        renderizar();

        expect(await screen.findByText('No encontramos alojamientos con esos criterios.')).toBeInTheDocument();
        expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Todos los destinos: 0 alojamientos');
    });

    it('si falla la carga muestra un error comprensible', async () => {
        vi.mocked(fetchDestinos).mockRejectedValue(new Error('red'));
        renderizar();

        expect(await screen.findByRole('alert')).toHaveTextContent('No pudimos cargar los alojamientos.');
    });
});
