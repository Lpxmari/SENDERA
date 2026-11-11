import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { fetchDestinos } from '../services/destinoService';
import Home from './Home';

const paginaVacia = { contenido: [], pagina: 0, tamano: 6, totalElementos: 0, totalPaginas: 0 };

vi.mock('../services/destinoService', () => ({ fetchDestinos: vi.fn() }));


const renderizar = (ruta = '/') =>
    render(
        <MemoryRouter initialEntries={[ruta]}>
            <Home />
        </MemoryRouter>,
    );

describe('Home: enlaces a una sección (#ancla)', () => {
    beforeEach(() => {
        vi.mocked(fetchDestinos).mockReset();
        vi.mocked(fetchDestinos).mockResolvedValue(paginaVacia);
    });

    afterEach(() => {
        // jsdom no implementa scrollIntoView
        delete (Element.prototype as { scrollIntoView?: unknown }).scrollIntoView;
    });

    it('baja a la sección cuando llega con /#destinos-populares y la carga terminó', async () => {
        const scrollIntoView = vi.fn();
        Element.prototype.scrollIntoView = scrollIntoView;
        // La sección "Destinos populares" solo existe si hay destinos que mostrar
        vi.mocked(fetchDestinos).mockResolvedValueOnce({
            contenido: [
                { id: 1, nombre: 'Filandia', descripcion: 'Pueblo cafetero', ubicacion: 'Filandia, Quindío', precioBase: 100000, dificultad: 'BAJA', accesibilidad: true, fotosUrl: [] },
            ],
            pagina: 0,
            tamano: 6,
            totalElementos: 1,
            totalPaginas: 1,
        });

        renderizar('/#destinos-populares');

        await waitFor(() => expect(scrollIntoView).toHaveBeenCalledTimes(1));
        expect((scrollIntoView.mock.contexts[0] as HTMLElement).id).toBe('destinos-populares');
    });

    it('no se desplaza si no llega con ancla', async () => {
        const scrollIntoView = vi.fn();
        Element.prototype.scrollIntoView = scrollIntoView;

        renderizar('/');
        await screen.findByRole('heading', { name: 'Busca tu próximo destino' });

        expect(scrollIntoView).not.toHaveBeenCalled();
    });
});

describe('Home: pestaña de restaurantes', () => {
    it('lista los restaurantes ofertados al elegir la pestaña', async () => {
        renderizar();

        fireEvent.click(screen.getByRole('tab', { name: 'Restaurantes' }));

        expect(await screen.findByRole('heading', { name: 'Restaurantes ofertados' })).toBeInTheDocument();
        expect(await screen.findByText('La Cocina del Cocora')).toBeInTheDocument();
        expect(screen.getAllByRole('article')).toHaveLength(6);
    });

    it('filtra los restaurantes con la búsqueda del encabezado', async () => {
        renderizar('/?destino=cartagena');

        fireEvent.click(screen.getByRole('tab', { name: 'Restaurantes' }));

        expect(await screen.findByText('Casa del Puerto')).toBeInTheDocument();
        expect(screen.getAllByRole('article')).toHaveLength(1);
    });

    it('vuelve a alojamiento sin mostrar restaurantes', async () => {
        renderizar();

        fireEvent.click(screen.getByRole('tab', { name: 'Restaurantes' }));
        await screen.findByText('La Cocina del Cocora');
        fireEvent.click(screen.getByRole('tab', { name: 'Alojamiento' }));

        expect(screen.queryByText('La Cocina del Cocora')).not.toBeInTheDocument();
        expect(screen.getByRole('heading', { name: 'Busca tu próximo destino' })).toBeInTheDocument();
    });
});

describe('Home: solo pide la primera página (OB-01)', () => {
    beforeEach(() => {
        vi.mocked(fetchDestinos).mockReset();
        vi.mocked(fetchDestinos).mockResolvedValue(paginaVacia);
    });

    it('pide la página 0 con lo que muestra la portada (6 destinos)', async () => {
        renderizar('/');
        await screen.findByText(/No encontramos destinos/);

        expect(fetchDestinos).toHaveBeenCalledTimes(1);
        expect(fetchDestinos).toHaveBeenCalledWith({ q: '', page: 0, size: 6 });
    });

    it('envía la búsqueda del encabezado al backend como q', async () => {
        renderizar('/?destino=%20Salento%20');
        await screen.findByText(/No encontramos destinos/);

        expect(fetchDestinos).toHaveBeenCalledWith({ q: 'Salento', page: 0, size: 6 });
    });

    it('reparte los destinos de la página en destacado, ofertas y populares', async () => {
        const destino = (id: number) => ({
            id,
            nombre: `Destino ${id}`,
            descripcion: 'x',
            ubicacion: 'Colombia',
            precioBase: 100000,
            dificultad: 'BAJA' as const,
            accesibilidad: true,
            fotosUrl: [],
        });
        vi.mocked(fetchDestinos).mockResolvedValue({
            contenido: [1, 2, 3, 4, 5, 6].map(destino),
            pagina: 0,
            tamano: 6,
            totalElementos: 40,
            totalPaginas: 7,
        });

        renderizar('/');

        expect(await screen.findByRole('heading', { name: 'Ofertas' })).toBeInTheDocument();
        expect(screen.getByRole('heading', { name: 'Destinos populares' })).toBeInTheDocument();
        expect(screen.getByRole('heading', { name: 'Destino 1' })).toBeInTheDocument();
    });

    it('muestra un error comprensible si falla la carga', async () => {
        vi.mocked(fetchDestinos).mockRejectedValue(new Error('red'));

        renderizar('/');

        expect(await screen.findByRole('alert')).toHaveTextContent('No pudimos cargar los destinos.');
    });
});
