import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { AuthProvider } from '../../context/AuthContext';
import Encabezado from '../Encabezado';
import { guardarSesion, leerSesion } from '../../services/sesion';
import { RolUsuario } from '../../types/auth';
import RutaCliente from './RutaCliente';

const abrirSesion = (rol: RolUsuario) =>
    guardarSesion({
        token: 'token-de-prueba',
        usuario: { id: 1, nombre: 'Ana', apellidos: 'Pérez', correo: 'ana@correo.com', rol },
    });

describe('RutaCliente', () => {
    const renderizar = () =>
        render(
            <MemoryRouter initialEntries={['/perfil']}>
                <AuthProvider>
                    <Routes>
                        <Route element={<RutaCliente />}>
                            <Route path="/perfil" element={<p>Mi perfil de viajero</p>} />
                        </Route>
                        <Route path="/admin" element={<p>Panel de administración</p>} />
                    </Routes>
                </AuthProvider>
            </MemoryRouter>,
        );

    it('deja ver el perfil a un viajero', () => {
        abrirSesion('ROLE_CLIENTE');
        renderizar();

        expect(screen.getByText('Mi perfil de viajero')).toBeInTheDocument();
    });

    it('lleva a un administrador a su panel en vez de al perfil de viajero', () => {
        abrirSesion('ROLE_ADMIN');
        renderizar();

        expect(screen.getByText('Panel de administración')).toBeInTheDocument();
        expect(screen.queryByText('Mi perfil de viajero')).not.toBeInTheDocument();
    });
});

describe('Encabezado según el rol', () => {
    const renderizar = (ruta = '/') =>
        render(
            <MemoryRouter initialEntries={[ruta]}>
                <AuthProvider>
                    <Encabezado />
                </AuthProvider>
            </MemoryRouter>,
        );

    it('un viajero ve su menú de cuenta y no "Administración"', () => {
        abrirSesion('ROLE_CLIENTE');
        renderizar();

        expect(screen.getByRole('button', { name: 'Mi cuenta, Ana' })).toBeInTheDocument();
        expect(screen.queryByRole('link', { name: 'Administración' })).not.toBeInTheDocument();
    });

    it('el menú del viajero despliega "Ver mi perfil" y el correo al pulsarlo', () => {
        abrirSesion('ROLE_CLIENTE');
        renderizar();
        expect(screen.queryByRole('link', { name: 'Ver mi perfil' })).not.toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', { name: 'Mi cuenta, Ana' }));

        expect(screen.getByRole('link', { name: 'Ver mi perfil' })).toHaveAttribute('href', '/perfil');
        expect(screen.getByText('ana@correo.com')).toBeInTheDocument();

        fireEvent.keyDown(document, { key: 'Escape' });
        expect(screen.queryByRole('link', { name: 'Ver mi perfil' })).not.toBeInTheDocument();
    });

    it('"Cerrar sesión" del viajero está dentro del menú y cierra la sesión', () => {
        abrirSesion('ROLE_CLIENTE');
        renderizar();
        expect(screen.queryByRole('button', { name: 'Cerrar sesión' })).not.toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', { name: 'Mi cuenta, Ana' }));
        fireEvent.click(screen.getByRole('button', { name: 'Cerrar sesión' }));

        expect(leerSesion()).toBeNull();
        expect(screen.getByRole('link', { name: 'Iniciar Sesión' })).toBeInTheDocument();
    });

    it('el administrador ve el mismo menú de cuenta, con "Administración" en vez de "Ver mi perfil"', () => {
        abrirSesion('ROLE_ADMIN');
        renderizar();
        expect(screen.queryByRole('link', { name: 'Administración' })).not.toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', { name: 'Mi cuenta, Ana' }));

        expect(screen.getByRole('link', { name: 'Administración' })).toHaveAttribute('href', '/admin');
        expect(screen.queryByRole('link', { name: /perfil/i })).not.toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Cerrar sesión' })).toBeInTheDocument();
    });

    it('el enlace "Destinos" aparece solo en el inicio', () => {
        const { unmount } = renderizar('/');
        expect(screen.getByRole('link', { name: 'Destinos' })).toBeInTheDocument();
        unmount();

        for (const ruta of ['/resultados', '/destinos/1', '/perfil']) {
            const vista = renderizar(ruta);
            expect(screen.queryByRole('link', { name: 'Destinos' })).not.toBeInTheDocument();
            vista.unmount();
        }
    });
});
