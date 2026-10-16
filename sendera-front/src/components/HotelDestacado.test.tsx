import { render, screen, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { Destino } from '../types/destino';
import HotelDestacado from './HotelDestacado';

const DESTINO: Destino = {
    id: 1,
    nombre: 'Parque Tayrona',
    descripcion: 'Senderos costeros.',
    ubicacion: 'Santa Marta, Magdalena',
    precioBase: 145000,
    dificultad: 'MODERADA',
    accesibilidad: true,
    fotosUrl: [],
};

const renderizar = (destino: Destino) =>
    render(
        <MemoryRouter>
            <HotelDestacado destino={destino} />
        </MemoryRouter>,
    );

describe('HotelDestacado', () => {
    it('muestra ubicación, accesibilidad, dificultad, precio y servicios', () => {
        renderizar(DESTINO);

        const datos = within(screen.getByRole('list', { name: 'Datos y servicios' }));
        expect(datos.getByText('Santa Marta, Magdalena')).toBeInTheDocument();
        expect(datos.getByText('Accesibilidad disponible')).toBeInTheDocument();
        expect(datos.getByText('Dificultad moderada')).toBeInTheDocument();
        expect(datos.getByText(/Desde/)).toBeInTheDocument();
        expect(datos.getByText('Wi-Fi')).toBeInTheDocument();
    });

    it('no anuncia accesibilidad si el destino no la tiene', () => {
        renderizar({ ...DESTINO, accesibilidad: false });

        expect(screen.queryByText('Accesibilidad disponible')).not.toBeInTheDocument();
    });
});
