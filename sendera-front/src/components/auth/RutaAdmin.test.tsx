import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { AuthProvider } from '../../context/AuthContext';
import { guardarSesion } from '../../services/sesion';
import { RolUsuario } from '../../types/auth';
import RutaAdmin from './RutaAdmin';

const renderizar = () =>
    render(
        <MemoryRouter initialEntries={['/admin']}>
            <AuthProvider>
                <Routes>
                    <Route element={<RutaAdmin />}>
                        <Route path="/admin" element={<p>Zona de administración</p>} />
                    </Route>
                    <Route path="/" element={<p>Inicio</p>} />
                    <Route path="/login" element={<p>Pantalla de ingreso</p>} />
                </Routes>
            </AuthProvider>
        </MemoryRouter>,
    );

const abrirSesion = (rol?: RolUsuario) =>
    guardarSesion({
        token: 'token-de-prueba',
        usuario: { id: 1, nombre: 'Ana', apellidos: 'Pérez', correo: 'ana@correo.com', rol },
    });

describe('RutaAdmin', () => {
    it('envía a iniciar sesión a quien no la tiene', () => {
        renderizar();

        expect(screen.getByText('Pantalla de ingreso')).toBeInTheDocument();
    });

    it('envía al inicio a un cliente', () => {
        abrirSesion('ROLE_CLIENTE');
        renderizar();

        expect(screen.getByText('Inicio')).toBeInTheDocument();
        expect(screen.queryByText('Zona de administración')).not.toBeInTheDocument();
    });

    it('trata una sesión antigua sin rol como no administrador', () => {
        abrirSesion(undefined);
        renderizar();

        expect(screen.getByText('Inicio')).toBeInTheDocument();
    });

    it('deja pasar a un administrador', () => {
        abrirSesion('ROLE_ADMIN');
        renderizar();

        expect(screen.getByText('Zona de administración')).toBeInTheDocument();
    });
});
