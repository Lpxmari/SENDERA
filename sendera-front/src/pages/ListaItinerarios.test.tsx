import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fetchViajes } from '../services/viajeService';
import { ViajeResumen } from '../types/viaje';
import ListaItinerarios from './ListaItinerarios';

vi.mock('../services/viajeService');

const viajes: ViajeResumen[] = [
    { id: 1, nombre: 'Viaje al Quindío', ubicacion: 'Filandia, Quindío', fechaInicio: '2099-10-15', fechaFin: '2099-10-18', totalActividades: 3, primeraHora: '09:00' },
    { id: 2, nombre: 'Recorrido de Fauna', ubicacion: 'Leticia, Amazonas', fechaInicio: '2099-10-15', fechaFin: '2099-10-15', totalActividades: 1, primeraHora: '09:00' },
    { id: 3, nombre: 'Una actividad, varios días', ubicacion: 'Isla Barú, Bolívar', fechaInicio: '2099-11-20', fechaFin: '2099-11-22', totalActividades: 1, primeraHora: '15:30' },
    { id: 4, nombre: 'Sin actividades', ubicacion: null, fechaInicio: '2099-12-01', fechaFin: '2099-12-01', totalActividades: 0, primeraHora: null },
];

const renderizar = (estado?: { guardado?: string; eliminado?: string }) =>
    render(
        <MemoryRouter initialEntries={[{ pathname: '/itinerarios', state: estado }]}>
            <Routes>
                <Route path="/itinerarios" element={<ListaItinerarios />} />
                <Route path="/itinerarios/nuevo" element={<p>Crear</p>} />
            </Routes>
        </MemoryRouter>,
    );

describe('ListaItinerarios', () => {
    beforeEach(() => {
        vi.resetAllMocks();
    });

    it('muestra cada itinerario con su ubicación, fechas y número de actividades', async () => {
        vi.mocked(fetchViajes).mockResolvedValue(viajes);
        renderizar();

        expect(await screen.findByRole('heading', { name: 'Viaje al Quindío' })).toBeInTheDocument();
        expect(screen.getByText('Filandia, Quindío')).toBeInTheDocument();
        expect(screen.getByText('15 - 18 de octubre de 2099')).toBeInTheDocument();
        expect(screen.getByText('3 actividades')).toBeInTheDocument();
        expect(screen.getAllByText('1 actividad')).toHaveLength(2);
        expect(screen.getByText('0 actividades')).toBeInTheDocument();
    });

    it('muestra la hora junto a la fecha solo si el viaje es de un día y una actividad', async () => {
        vi.mocked(fetchViajes).mockResolvedValue(viajes);
        renderizar();

        await screen.findByRole('heading', { name: 'Recorrido de Fauna' });
        expect(screen.getByText('15 de octubre de 2099 · 9:00 a. m.')).toBeInTheDocument();
        expect(screen.getByText('20 - 22 de noviembre de 2099')).toBeInTheDocument();
        expect(screen.queryByText(/3:30 p\. m\./)).not.toBeInTheDocument();
        expect(screen.getByText('15 - 18 de octubre de 2099')).toBeInTheDocument();
        expect(screen.getByText('1 de diciembre de 2099')).toBeInTheDocument();
    });

    it('enlaza a editar cada itinerario y a crear uno nuevo', async () => {
        vi.mocked(fetchViajes).mockResolvedValue(viajes);
        renderizar();

        await screen.findByRole('heading', { name: 'Viaje al Quindío' });
        expect(screen.getByRole('link', { name: 'Editar Viaje al Quindío' })).toHaveAttribute('href', '/itinerarios/1/editar');
        expect(screen.getByRole('link', { name: 'Ver itinerario de Viaje al Quindío' })).toHaveAttribute('href', '/itinerarios/1');
        expect(screen.getByRole('link', { name: '+ Crear nuevo itinerario' })).toHaveAttribute('href', '/itinerarios/nuevo');
    });

    it('avisa cuando todavía no hay itinerarios', async () => {
        vi.mocked(fetchViajes).mockResolvedValue([]);
        renderizar();

        expect(await screen.findByText(/Aún no tienes itinerarios/)).toBeInTheDocument();
    });

    it('muestra un mensaje comprensible si falla la carga', async () => {
        vi.mocked(fetchViajes).mockRejectedValue(new Error('red caída'));
        renderizar();

        expect(await screen.findByRole('alert')).toHaveTextContent('No se pudieron cargar tus itinerarios');
    });

    it('confirma el guardado cuando llega desde crear o editar', async () => {
        vi.mocked(fetchViajes).mockResolvedValue(viajes);
        renderizar({ guardado: 'Viaje al Quindío' });

        expect(await screen.findByRole('status')).toHaveTextContent('Guardaste el itinerario «Viaje al Quindío».');
    });

    it('el aviso se muestra una sola vez: se borra del estado de navegación para que no reaparezca al recargar', async () => {
        vi.mocked(fetchViajes).mockResolvedValue(viajes);
        const Espia = () => <p data-testid="estado-navegacion">{useLocation().state === null ? 'vacío' : 'con aviso'}</p>;
        render(
            <MemoryRouter initialEntries={[{ pathname: '/itinerarios', state: { guardado: 'Viaje al Quindío' } }]}>
                <Espia />
                <Routes>
                    <Route path="/itinerarios" element={<ListaItinerarios />} />
                </Routes>
            </MemoryRouter>,
        );

        expect(await screen.findByRole('status')).toHaveTextContent('Guardaste el itinerario «Viaje al Quindío».');
        await waitFor(() => expect(screen.getByTestId('estado-navegacion')).toHaveTextContent('vacío'));
        expect(screen.getByRole('status')).toHaveTextContent('Guardaste el itinerario');
    });

    it('confirma la eliminación cuando llega desde el detalle', async () => {
        vi.mocked(fetchViajes).mockResolvedValue([]);
        renderizar({ eliminado: 'Viaje al Quindío' });

        expect(await screen.findByText('Eliminaste el itinerario «Viaje al Quindío».')).toBeInTheDocument();
    });
});
