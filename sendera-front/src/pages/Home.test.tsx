import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import Home from './Home';

vi.mock('../services/destinoService', () => ({ fetchDestinos: vi.fn().mockResolvedValue([]) }));

const renderizar = (ruta = '/') =>
    render(
        <MemoryRouter initialEntries={[ruta]}>
            <Home />
        </MemoryRouter>,
    );

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
