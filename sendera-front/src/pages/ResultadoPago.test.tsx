import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { iniciarPago, verificarPago } from '../services/reservaService';
import { Reserva } from '../types/reserva';
import ResultadoPago from './ResultadoPago';

vi.mock('../services/reservaService', () => ({ verificarPago: vi.fn(), iniciarPago: vi.fn() }));

const RESERVA: Reserva = {
    id: 15,
    itinerarioId: 2,
    fechaReserva: '2026-11-18',
    montoTotal: 195000,
    estadoReserva: 'PENDIENTE',
    estadoPago: 'PENDIENTE',
    destinoNombre: 'Finca Hotel Los Arrayanes',
    itinerarioNombre: 'Descanso cafetero',
};

const renderizar = (url = '/pago/resultado?reserva=15&status=approved&payment_id=987') =>
    render(
        <MemoryRouter initialEntries={[url]}>
            <ResultadoPago />
        </MemoryRouter>,
    );

describe('ResultadoPago', () => {
    const assignOriginal = window.location.assign;
    const assign = vi.fn();

    beforeEach(() => {
        Object.defineProperty(window, 'location', { value: { ...window.location, assign }, writable: true });
    });

    afterEach(() => {
        Object.defineProperty(window, 'location', {
            value: { ...window.location, assign: assignOriginal },
            writable: true,
        });
    });

    it('consulta el estado real en el backend y muestra el pago aprobado', async () => {
        vi.mocked(verificarPago).mockResolvedValue({ ...RESERVA, estadoReserva: 'CONFIRMADA', estadoPago: 'APROBADO' });

        renderizar();

        expect(await screen.findByRole('heading', { name: '¡Pago aprobado!' })).toBeInTheDocument();
        expect(verificarPago).toHaveBeenCalledWith(15);
        expect(screen.getByText('Finca Hotel Los Arrayanes')).toBeInTheDocument();
        expect(screen.getByText(/Confirmada/)).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /Pagar/ })).not.toBeInTheDocument();
    });

    it('no se fía del parámetro status de la URL: si el backend dice rechazado, ofrece reintentar', async () => {
        vi.mocked(verificarPago).mockResolvedValue({ ...RESERVA, estadoPago: 'RECHAZADO' });
        vi.mocked(iniciarPago).mockResolvedValue({ preferenciaId: 'p2', urlPago: 'https://mp.test/checkout/p2' });

        renderizar();

        expect(await screen.findByRole('heading', { name: 'El pago no se completó' })).toBeInTheDocument();
        fireEvent.click(screen.getByRole('button', { name: 'Intentar pagar de nuevo' }));

        await waitFor(() => expect(assign).toHaveBeenCalledWith('https://mp.test/checkout/p2'));
        expect(iniciarPago).toHaveBeenCalledWith(15);
    });

    it('con el pago en proceso permite volver a verificar', async () => {
        vi.mocked(verificarPago)
            .mockResolvedValueOnce(RESERVA)
            .mockResolvedValueOnce({ ...RESERVA, estadoReserva: 'CONFIRMADA', estadoPago: 'APROBADO' });

        renderizar();

        expect(await screen.findByRole('heading', { name: 'Tu pago está en proceso' })).toBeInTheDocument();
        fireEvent.click(screen.getByRole('button', { name: 'Volver a verificar' }));

        expect(await screen.findByRole('heading', { name: '¡Pago aprobado!' })).toBeInTheDocument();
        expect(verificarPago).toHaveBeenCalledTimes(2);
    });

    it('muestra el mensaje del backend si no se puede abrir el pago', async () => {
        vi.mocked(verificarPago).mockResolvedValue({ ...RESERVA, estadoPago: null });
        vi.mocked(iniciarPago).mockRejectedValue(new Error('red'));

        renderizar();

        fireEvent.click(await screen.findByRole('button', { name: 'Pagar con Mercado Pago' }));

        expect(await screen.findByRole('alert')).toHaveTextContent('No pudimos abrir el pago. Intenta de nuevo.');
        expect(assign).not.toHaveBeenCalled();
    });

    it('sin un id de reserva válido no consulta al backend', () => {
        renderizar('/pago/resultado?status=approved');

        expect(screen.getByRole('alert')).toHaveTextContent('No encontramos la reserva de este pago.');
        expect(verificarPago).not.toHaveBeenCalled();
    });

    it('si la consulta falla muestra el error y deja volver a las reservas', async () => {
        vi.mocked(verificarPago).mockRejectedValue(new Error('red'));

        renderizar();

        expect(await screen.findByRole('alert')).toHaveTextContent('No pudimos consultar el estado de tu pago.');
        expect(screen.getByRole('link', { name: 'Mis reservas' })).toHaveAttribute('href', '/perfil?seccion=reservas');
    });
});
