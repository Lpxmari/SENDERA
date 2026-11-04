import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { Reserva } from '../types/reserva';
import ConfirmacionReserva from './ConfirmacionReserva';

const RESERVA: Reserva = {
    id: 9,
    itinerarioId: 21,
    fechaReserva: '2026-11-18',
    montoTotal: 195000,
    estadoReserva: 'PENDIENTE',
    destinoNombre: 'Finca Hotel Los Arrayanes',
    itinerarioNombre: 'Ruta del Café',
};

const renderizar = (state?: { reserva: Reserva }) =>
    render(
        <MemoryRouter initialEntries={[{ pathname: '/confirmacion', state }]}>
            <ConfirmacionReserva />
        </MemoryRouter>,
    );

describe('ConfirmacionReserva', () => {
    it('muestra la reserva creada con su estado en texto', () => {
        renderizar({ reserva: RESERVA });

        expect(screen.getByRole('heading', { name: '¡Reserva creada!' })).toBeInTheDocument();
        expect(screen.getByText('Finca Hotel Los Arrayanes')).toBeInTheDocument();
        expect(screen.getByText('Ruta del Café')).toBeInTheDocument();
        expect(screen.getByText('18 de noviembre de 2026')).toBeInTheDocument();
        expect(screen.getByText(/195\.000/)).toBeInTheDocument();
        expect(screen.getByText(/Pendiente de confirmación/)).toBeInTheDocument();
    });

    it('enlaza a "Mis reservas" en el perfil', () => {
        renderizar({ reserva: RESERVA });

        expect(screen.getByRole('link', { name: 'Mis reservas' })).toHaveAttribute('href', '/perfil?seccion=reservas');
    });

    it('si se entra directo, sin reserva, muestra el aviso general y el mismo enlace', () => {
        renderizar();

        expect(screen.getByText(/Tu reserva quedó registrada/)).toBeInTheDocument();
        expect(screen.queryByText(/Pendiente de confirmación/)).not.toBeInTheDocument();
        expect(screen.getByRole('link', { name: 'Mis reservas' })).toHaveAttribute('href', '/perfil?seccion=reservas');
    });
});
