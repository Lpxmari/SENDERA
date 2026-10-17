import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import { AuthProvider } from '../context/AuthContext';
import DetalleDestino from './DetalleDestino';

vi.mock('../services/destinoService', () => ({
    getDestinoPorId: vi.fn().mockResolvedValue({
        id: 1,
        nombre: 'Hotel Boutique Casa Aurora',
        descripcion: 'Casona colonial',
        ubicacion: 'Cartagena, Bolívar',
        precioBase: 320000,
        dificultad: 'BAJA',
        accesibilidad: true,
        fotosUrl: [
            '/imagenes/alojamientos/casa-aurora-1.jpg',
            '/imagenes/alojamientos/casa-aurora-2.jpg',
            '/imagenes/alojamientos/casa-aurora-3.jpg',
            '/imagenes/alojamientos/casa-aurora-4.jpg',
            '/imagenes/alojamientos/casa-aurora-5.jpg',
        ],
    }),
}));
vi.mock('../services/resenaService', () => ({
    listarResenas: vi.fn().mockResolvedValue([]),
    crearResena: vi.fn(),
    obtenerUrlImagenResena: (ruta: string) => ruta,
}));

const renderizar = () =>
    render(
        <MemoryRouter initialEntries={['/destinos/1']}>
            <AuthProvider>
                <Routes>
                    <Route path="/destinos/:id" element={<DetalleDestino />} />
                </Routes>
            </AuthProvider>
        </MemoryRouter>,
    );

describe('Detalle del alojamiento: galería', () => {
    it('muestra todas las fotos como miniaturas y la primera como principal', async () => {
        renderizar();

        const miniaturas = await screen.findAllByRole('button', { name: /Ver foto \d de Hotel Boutique Casa Aurora/ });
        expect(miniaturas).toHaveLength(5);
        expect(screen.getByAltText('Foto 1 de Hotel Boutique Casa Aurora')).toHaveAttribute(
            'src',
            '/imagenes/alojamientos/casa-aurora-1.jpg',
        );
        expect(miniaturas[0]).toHaveAttribute('aria-pressed', 'true');
    });

    it('al pulsar una miniatura cambia la foto principal', async () => {
        renderizar();

        fireEvent.click(await screen.findByRole('button', { name: 'Ver foto 4 de Hotel Boutique Casa Aurora' }));

        expect(screen.getByAltText('Foto 4 de Hotel Boutique Casa Aurora')).toHaveAttribute(
            'src',
            '/imagenes/alojamientos/casa-aurora-4.jpg',
        );
        expect(screen.getByRole('button', { name: 'Ver foto 4 de Hotel Boutique Casa Aurora' })).toHaveAttribute(
            'aria-pressed',
            'true',
        );
    });
});
