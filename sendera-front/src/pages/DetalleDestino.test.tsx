import { render, screen, within } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthProvider } from '../context/AuthContext';
import { getDestinoPorId } from '../services/destinoService';
import { Destino } from '../types/destino';
import DetalleDestino from './DetalleDestino';

vi.mock('../services/destinoService', () => ({ getDestinoPorId: vi.fn() }));
vi.mock('../services/resenaService', () => ({
    listarResenas: vi.fn().mockResolvedValue([]),
    crearResena: vi.fn(),
    obtenerUrlImagenResena: (ruta: string) => ruta,
}));

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

const renderizar = (destino: Destino) => {
    vi.mocked(getDestinoPorId).mockResolvedValue(destino);
    return render(
        <MemoryRouter initialEntries={['/destinos/1']}>
            <AuthProvider>
                <Routes>
                    <Route path="/destinos/:id" element={<DetalleDestino />} />
                </Routes>
            </AuthProvider>
        </MemoryRouter>,
    );
};

describe('DetalleDestino: dificultad y accesibilidad (OM-06)', () => {
    beforeEach(() => vi.clearAllMocks());

    it('muestra precio, dificultad y accesibilidad con texto', async () => {
        renderizar(DESTINO);

        const datos = within(await screen.findByRole('list', { name: 'Datos del destino' }));
        expect(datos.getByText(/Desde/)).toHaveTextContent('145.000');
        expect(datos.getByText('Dificultad: Moderada')).toBeInTheDocument();
        expect(datos.getByText('Accesible')).toBeInTheDocument();
    });

    it.each([
        ['BAJA', 'Dificultad: Baja'],
        ['MODERADA', 'Dificultad: Moderada'],
        ['ALTA', 'Dificultad: Alta'],
    ] as const)('traduce la dificultad %s', async (dificultad, texto) => {
        renderizar({ ...DESTINO, dificultad });

        expect(await screen.findByText(texto)).toBeInTheDocument();
    });

    it('indica con texto cuando el destino no es accesible', async () => {
        renderizar({ ...DESTINO, accesibilidad: false });

        const datos = within(await screen.findByRole('list', { name: 'Datos del destino' }));
        expect(datos.getByText('No accesible')).toBeInTheDocument();
        expect(datos.queryByText('Accesible')).not.toBeInTheDocument();
    });

    it('queda en la cabecera, antes de la galería y de la tarjeta de reserva', async () => {
        renderizar(DESTINO);

        const datos = await screen.findByRole('list', { name: 'Datos del destino' });
        const galeria = screen.getByRole('region', { name: 'Fotos del alojamiento' });
        expect(datos.compareDocumentPosition(galeria) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    });
});
