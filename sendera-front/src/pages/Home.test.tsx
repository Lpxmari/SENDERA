import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { fetchDestinos } from '../services/destinoService';
import Home from './Home';

vi.mock('../services/destinoService', () => ({ fetchDestinos: vi.fn().mockResolvedValue([]) }));

const renderizar = (ruta = '/') =>
    render(
        <MemoryRouter initialEntries={[ruta]}>
            <Home />
        </MemoryRouter>,
    );

describe('Home: enlaces a una sección (#ancla)', () => {
    afterEach(() => {
        // jsdom no implementa scrollIntoView
        delete (Element.prototype as { scrollIntoView?: unknown }).scrollIntoView;
    });

    it('baja a la sección cuando llega con /#destinos-populares y la carga terminó', async () => {
        const scrollIntoView = vi.fn();
        Element.prototype.scrollIntoView = scrollIntoView;
        // La sección "Destinos populares" solo existe si hay destinos que mostrar
        vi.mocked(fetchDestinos).mockResolvedValueOnce([
            { id: 1, nombre: 'Filandia', descripcion: 'Pueblo cafetero', ubicacion: 'Filandia, Quindío', precioBase: 100000, dificultad: 'BAJA', accesibilidad: true, fotosUrl: [] },
        ]);

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
