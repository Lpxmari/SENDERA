import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { eliminarViaje, fetchViaje } from '../services/viajeService';
import { Viaje } from '../types/viaje';
import DetalleItinerario from './DetalleItinerario';

vi.mock('../services/viajeService');

// 2099-10-15 es jueves
const viaje: Viaje = {
    id: 7,
    nombre: 'Viaje al Quindío',
    fechaInicio: '2099-10-15',
    fechaFin: '2099-10-18',
    actividades: [
        { id: 1, actividadId: 1, destinoId: 10, nombre: 'Recorrido por Filandia', ubicacion: 'Filandia, Quindío', fecha: '2099-10-15', horaInicio: '09:00', duracionMinutos: 180, dificultad: 'BAJA', orden: 1 },
        { id: 2, actividadId: 2, destinoId: 10, nombre: 'Visita a finca cafetera', ubicacion: 'Quindío', fecha: '2099-10-15', horaInicio: '14:00', duracionMinutos: 120, dificultad: 'MODERADA', orden: 2 },
        { id: 3, actividadId: 3, destinoId: 10, nombre: 'Experiencia gastronómica', ubicacion: 'Filandia, Quindío', fecha: '2099-10-16', horaInicio: '10:00', duracionMinutos: 120, dificultad: 'BAJA', orden: 1 },
    ],
};

const renderizar = () =>
    render(
        <MemoryRouter initialEntries={['/itinerarios/7']}>
            <Routes>
                <Route path="/itinerarios/:id" element={<DetalleItinerario />} />
                <Route path="/itinerarios" element={<p>Lista de itinerarios</p>} />
                <Route path="/itinerarios/:id/editar" element={<p>Formulario de edición</p>} />
            </Routes>
        </MemoryRouter>,
    );

describe('DetalleItinerario', () => {
    beforeEach(() => {
        vi.resetAllMocks();
    });

    it('muestra el viaje con la ubicación, las fechas y las actividades de cada día con su hora', async () => {
        vi.mocked(fetchViaje).mockResolvedValue(viaje);
        renderizar();

        expect(await screen.findByRole('heading', { level: 1, name: 'Viaje al Quindío' })).toBeInTheDocument();
        const datos = screen.getByRole('region', { name: 'Datos del viaje' });
        expect(within(datos).getByText('Filandia, Quindío')).toBeInTheDocument();
        expect(within(datos).getByText('15 - 18 de octubre de 2099')).toBeInTheDocument();

        const dia1 = screen.getByRole('region', { name: 'Día 1 - Jueves 15 de octubre de 2099' });
        expect(within(dia1).getAllByRole('heading', { level: 4 }).map((h) => h.textContent)).toEqual([
            'Recorrido por Filandia',
            'Visita a finca cafetera',
        ]);
        expect(within(dia1).getByText('9:00 a. m.')).toBeInTheDocument();
        expect(within(dia1).getByText('2:00 p. m.')).toBeInTheDocument();
        const dia2 = screen.getByRole('region', { name: 'Día 2 - Viernes 16 de octubre de 2099' });
        expect(within(dia2).getByText('10:00 a. m.')).toBeInTheDocument();
    });

    it('no muestra los días sin actividades', async () => {
        vi.mocked(fetchViaje).mockResolvedValue(viaje);
        renderizar();

        await screen.findByRole('heading', { level: 1 });
        expect(screen.queryByRole('region', { name: /Día 3/ })).not.toBeInTheDocument();
    });

    it('avisa cuando el itinerario aún no tiene actividades', async () => {
        vi.mocked(fetchViaje).mockResolvedValue({ ...viaje, actividades: [] });
        renderizar();

        expect(await screen.findByText('Este itinerario aún no tiene actividades.')).toBeInTheDocument();
    });

    it('muestra un mensaje comprensible si no se puede cargar', async () => {
        vi.mocked(fetchViaje).mockRejectedValue(new Error('red caída'));
        renderizar();

        expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo cargar el itinerario');
        expect(screen.getByRole('link', { name: 'Volver a Mis itinerarios' })).toHaveAttribute('href', '/itinerarios');
    });

    it('lleva a editar el itinerario', async () => {
        vi.mocked(fetchViaje).mockResolvedValue(viaje);
        renderizar();

        const editar = await screen.findByRole('link', { name: 'Editar itinerario' });

        expect(editar).toHaveAttribute('href', '/itinerarios/7/editar');
    });

    it('pide confirmación antes de eliminar y no borra si se cancela', async () => {
        vi.mocked(fetchViaje).mockResolvedValue(viaje);
        renderizar();

        fireEvent.click(await screen.findByRole('button', { name: 'Eliminar itinerario' }));
        const dialogo = screen.getByRole('alertdialog');
        expect(dialogo).toHaveTextContent('¿Seguro que quieres eliminar «Viaje al Quindío»?');
        fireEvent.click(within(dialogo).getByRole('button', { name: 'Cancelar' }));

        expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
        expect(eliminarViaje).not.toHaveBeenCalled();
    });

    it('elimina el itinerario confirmado y vuelve a la lista', async () => {
        vi.mocked(fetchViaje).mockResolvedValue(viaje);
        vi.mocked(eliminarViaje).mockResolvedValue(undefined);
        renderizar();

        fireEvent.click(await screen.findByRole('button', { name: 'Eliminar itinerario' }));
        fireEvent.click(within(screen.getByRole('alertdialog')).getByRole('button', { name: 'Sí, eliminar' }));

        await waitFor(() => expect(eliminarViaje).toHaveBeenCalledWith(7));
        expect(await screen.findByText('Lista de itinerarios')).toBeInTheDocument();
    });

    it('si falla al eliminar, conserva la página y explica el problema', async () => {
        vi.mocked(fetchViaje).mockResolvedValue(viaje);
        vi.mocked(eliminarViaje).mockRejectedValue(new Error('red caída'));
        renderizar();

        fireEvent.click(await screen.findByRole('button', { name: 'Eliminar itinerario' }));
        fireEvent.click(within(screen.getByRole('alertdialog')).getByRole('button', { name: 'Sí, eliminar' }));

        expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo eliminar el itinerario');
        expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
        expect(screen.getByRole('heading', { level: 1, name: 'Viaje al Quindío' })).toBeInTheDocument();
    });
});
