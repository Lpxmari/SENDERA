import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it } from 'vitest';
import { AuthProvider } from '../context/AuthContext';
import { guardarSesion } from '../services/sesion';
import Encabezado from './Encabezado';

const renderizar = (variante?: 'completo' | 'itinerarios' | 'detalle') => {
    guardarSesion({ token: 'token', usuario: { id: 1, nombre: 'Ana', apellidos: 'Pérez', correo: 'ana@correo.com' } });
    return render(
        <MemoryRouter>
            <AuthProvider>
                <Encabezado variante={variante} />
            </AuthProvider>
        </MemoryRouter>,
    );
};

describe('Encabezado', () => {
    beforeEach(() => {
        localStorage.clear();
        sessionStorage.clear();
    });

    it('por defecto tiene el buscador de destinos', () => {
        renderizar();

        expect(screen.getByRole('search')).toBeInTheDocument();
        expect(screen.queryByRole('link', { name: 'Mi Perfil' })).not.toBeInTheDocument();
    });

    it('en las pantallas de itinerarios no tiene buscador y ofrece Mi Perfil, Destinos e Inicio', () => {
        renderizar('itinerarios');

        expect(screen.queryByRole('search')).not.toBeInTheDocument();
        expect(screen.getByRole('link', { name: 'Mi Perfil' })).toHaveAttribute('href', '/perfil');
        expect(screen.getByRole('link', { name: 'Destinos' })).toHaveAttribute('href', '/#destinos-populares');
        expect(screen.getByRole('link', { name: 'Inicio' })).toHaveAttribute('href', '/');
        expect(screen.getByRole('button', { name: /Mi cuenta/ })).toBeInTheDocument();
    });

    it('en el detalle solo tiene el logo y el botón para volver a Mi Perfil', () => {
        renderizar('detalle');

        expect(screen.queryByRole('search')).not.toBeInTheDocument();
        expect(screen.getByRole('link', { name: 'Mi Perfil' })).toHaveAttribute('href', '/perfil');
        expect(screen.getByRole('link', { name: 'Sendera, inicio' })).toBeInTheDocument();
        expect(screen.queryByRole('link', { name: 'Inicio' })).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /Mi cuenta/ })).not.toBeInTheDocument();
    });
});
