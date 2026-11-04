import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { listarItinerariosDeDestino } from '../services/destinoService';
import { crearReserva } from '../services/reservaService';
import { ItinerarioDisponible } from '../types/itinerario';
import { Reserva } from '../types/reserva';
import { hoyIso } from '../utils/fechasItinerario';
import FormularioReserva from './FormularioReserva';

vi.mock('../services/destinoService', () => ({ listarItinerariosDeDestino: vi.fn() }));
vi.mock('../services/reservaService', () => ({ crearReserva: vi.fn() }));

const CAFE: ItinerarioDisponible = {
    id: 21,
    nombre: 'Ruta del Café',
    fechaInicio: '2999-11-18',
    fechaFin: '2999-11-19',
    precioBase: 195000,
    cuposDisponibles: 8,
};
const PLAYA: ItinerarioDisponible = {
    id: 22,
    nombre: 'Fin de semana en la playa',
    fechaInicio: '2000-01-01', // ya empezó: el mínimo debe ser hoy
    fechaFin: '2999-12-31',
    precioBase: 410000,
    cuposDisponibles: 1,
};

/** Doble de la pantalla de confirmación: muestra lo que recibe por el estado de navegación. */
const Confirmacion = () => {
    const reserva = (useLocation().state as { reserva?: Reserva } | null)?.reserva;
    return (
        <div>
            <p>Reserva confirmada</p>
            <p>{reserva?.estadoReserva}</p>
        </div>
    );
};

const renderizar = () =>
    render(
        <MemoryRouter initialEntries={['/reservar/7']}>
            <Routes>
                <Route path="/reservar/:destinoId" element={<FormularioReserva />} />
                <Route path="/confirmacion" element={<Confirmacion />} />
            </Routes>
        </MemoryRouter>,
    );

const fecha = () => screen.getByLabelText(/Día de tu reserva/) as HTMLInputElement;
const botonEnviar = () => screen.getByRole('button', { name: 'Confirmar reserva' }) as HTMLButtonElement;

describe('FormularioReserva', () => {
    beforeEach(() => {
        vi.resetAllMocks();
    });

    it('muestra el estado de carga y luego los itinerarios del destino con fechas, precio y cupos', async () => {
        vi.mocked(listarItinerariosDeDestino).mockResolvedValue([CAFE, PLAYA]);
        renderizar();

        expect(screen.getByText(/Cargando fechas disponibles/)).toBeInTheDocument();
        expect(await screen.findByRole('radio', { name: /Ruta del Café/ })).toBeInTheDocument();
        expect(listarItinerariosDeDestino).toHaveBeenCalledWith(7);
        expect(screen.getByText('18 al 19 de noviembre de 2999')).toBeInTheDocument();
        expect(screen.getByText('Quedan 8 cupos')).toBeInTheDocument();
        expect(screen.getByText('Queda 1 cupo')).toBeInTheDocument();
        expect(screen.getByText(/195\.000/)).toBeInTheDocument();
    });

    it('sin itinerarios muestra un mensaje claro y no permite enviar', async () => {
        vi.mocked(listarItinerariosDeDestino).mockResolvedValue([]);
        renderizar();

        expect(await screen.findByText(/no tiene fechas disponibles por ahora/)).toBeInTheDocument();
        expect(screen.queryByRole('radio')).not.toBeInTheDocument();
        expect(botonEnviar().disabled).toBe(true);
        expect(fecha().disabled).toBe(true);
    });

    it('si falla la carga muestra el error y no permite enviar', async () => {
        vi.mocked(listarItinerariosDeDestino).mockRejectedValue(new Error('red'));
        renderizar();

        expect(await screen.findByRole('alert')).toHaveTextContent('No pudimos cargar las fechas disponibles.');
        expect(botonEnviar().disabled).toBe(true);
    });

    it('la fecha queda deshabilitada hasta elegir un itinerario y luego limita min y max a ese viaje', async () => {
        vi.mocked(listarItinerariosDeDestino).mockResolvedValue([CAFE, PLAYA]);
        renderizar();
        await screen.findByRole('radio', { name: /Ruta del Café/ });

        expect(fecha().disabled).toBe(true);
        expect(screen.getByText('Primero elige tu viaje.')).toBeInTheDocument();

        fireEvent.click(screen.getByRole('radio', { name: /Ruta del Café/ }));
        expect(fecha().disabled).toBe(false);
        expect(fecha().min).toBe('2999-11-18');
        expect(fecha().max).toBe('2999-11-19');
        expect(screen.getByText('✓ Elegido')).toBeInTheDocument();

        // Un viaje que ya empezó no permite fechas pasadas: min = hoy
        fireEvent.click(screen.getByRole('radio', { name: /Fin de semana en la playa/ }));
        expect(fecha().min).toBe(hoyIso());
        expect(fecha().max).toBe('2999-12-31');
    });

    it('al cambiar de viaje limpia la fecha si queda fuera del nuevo rango', async () => {
        vi.mocked(listarItinerariosDeDestino).mockResolvedValue([CAFE, PLAYA]);
        renderizar();
        fireEvent.click(await screen.findByRole('radio', { name: /Fin de semana en la playa/ }));
        fireEvent.change(fecha(), { target: { value: '2999-12-20' } });
        expect(fecha().value).toBe('2999-12-20');

        fireEvent.click(screen.getByRole('radio', { name: /Ruta del Café/ }));

        expect(fecha().value).toBe('');
    });

    it('envía el itinerarioId elegido (no el id del destino) con la fecha y va a la confirmación', async () => {
        vi.mocked(listarItinerariosDeDestino).mockResolvedValue([CAFE, PLAYA]);
        vi.mocked(crearReserva).mockResolvedValue({
            id: 1,
            itinerarioId: 21,
            fechaReserva: '2999-11-18',
            montoTotal: 195000,
            estadoReserva: 'PENDIENTE',
        });
        renderizar();

        fireEvent.click(await screen.findByRole('radio', { name: /Ruta del Café/ }));
        fireEvent.change(fecha(), { target: { value: '2999-11-18' } });
        fireEvent.click(botonEnviar());

        await waitFor(() =>
            expect(crearReserva).toHaveBeenCalledWith({ itinerarioId: 21, fechaReserva: '2999-11-18' }),
        );
        expect(await screen.findByText('Reserva confirmada')).toBeInTheDocument();
        expect(screen.getByText('PENDIENTE')).toBeInTheDocument(); // la reserva creada viaja a la confirmación
    });

    it('con un único itinerario lo deja elegido', async () => {
        vi.mocked(listarItinerariosDeDestino).mockResolvedValue([CAFE]);
        renderizar();

        const radio = (await screen.findByRole('radio', { name: /Ruta del Café/ })) as HTMLInputElement;
        expect(radio.checked).toBe(true);
        expect(fecha().disabled).toBe(false);
    });

    it('no habilita el envío sin fecha y muestra el error del servidor si la reserva falla', async () => {
        vi.mocked(listarItinerariosDeDestino).mockResolvedValue([CAFE]);
        vi.mocked(crearReserva).mockRejectedValue(new Error('conflicto'));
        renderizar();
        await screen.findByRole('radio', { name: /Ruta del Café/ });

        expect(botonEnviar().disabled).toBe(true);
        fireEvent.change(fecha(), { target: { value: '2999-11-18' } });
        expect(botonEnviar().disabled).toBe(false);
        fireEvent.click(botonEnviar());

        expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo crear la reserva. Intenta de nuevo.');
    });
});
