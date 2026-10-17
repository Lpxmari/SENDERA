import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import { listarReservasAdmin } from '../../services/adminService';
import { AuthProvider } from '../../context/AuthContext';
import { guardarSesion } from '../../services/sesion';
import AdminLayout from './AdminLayout';

vi.mock('../../services/adminService');
vi.mocked(listarReservasAdmin).mockResolvedValue({ contenido: [], pagina: 0, tamano: 1, total: 3 });

const renderizar = () => {
    guardarSesion({
        token: 'token-de-prueba',
        usuario: {
            id: 1,
            nombre: 'Ada',
            apellidos: 'Admin Sendera',
            correo: 'admin@sendera.com',
            rol: 'ROLE_ADMIN',
        },
    });
    return render(
        <MemoryRouter initialEntries={['/admin/usuarios']}>
            <AuthProvider>
                <Routes>
                    <Route path="/admin" element={<AdminLayout />}>
                        <Route path="usuarios" element={<p>Contenido de usuarios</p>} />
                    </Route>
                </Routes>
            </AuthProvider>
        </MemoryRouter>,
    );
};

describe('AdminLayout: menú de perfil', () => {
    it('muestra el menú principal con las cuatro secciones', () => {
        renderizar();

        const menu = screen.getByRole('navigation', { name: 'Administración' });
        for (const seccion of ['Usuarios', 'Destinos', 'Reservas', 'Reseñas']) {
            expect(menu).toHaveTextContent(seccion);
        }
    });

    it('al pulsar la foto muestra solo el nombre y el correo', () => {
        renderizar();
        expect(screen.queryByRole('region', { name: 'Perfil' })).not.toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', { name: 'Perfil de Ada' }));

        const panel = screen.getByRole('region', { name: 'Perfil' });
        expect(panel).toHaveTextContent('Ada Admin Sendera');
        expect(panel).toHaveTextContent('admin@sendera.com');
        expect(panel.querySelector('a')).toBeNull();
        expect(screen.getByRole('button', { name: 'Perfil de Ada' })).toHaveAttribute('aria-expanded', 'true');
    });

    it('no ofrece ninguna vista de "Mi perfil" de viajero', () => {
        renderizar();
        fireEvent.click(screen.getByRole('button', { name: 'Perfil de Ada' }));

        expect(screen.queryByRole('link', { name: /mi perfil/i })).not.toBeInTheDocument();
        expect(document.querySelector('a[href="/perfil"]')).toBeNull();
    });

    it('se cierra con Escape, con un clic fuera y al volver a pulsar la foto', () => {
        renderizar();
        const boton = screen.getByRole('button', { name: 'Perfil de Ada' });

        fireEvent.click(boton);
        fireEvent.keyDown(document, { key: 'Escape' });
        expect(screen.queryByRole('region', { name: 'Perfil' })).not.toBeInTheDocument();

        fireEvent.click(boton);
        fireEvent.mouseDown(screen.getByText('Contenido de usuarios'));
        expect(screen.queryByRole('region', { name: 'Perfil' })).not.toBeInTheDocument();

        fireEvent.click(boton);
        fireEvent.click(boton);
        expect(screen.queryByRole('region', { name: 'Perfil' })).not.toBeInTheDocument();
    });
});

describe('AdminLayout: barra lateral', () => {
    it('muestra la marca, el número de reservas pendientes y los accesos', async () => {
        renderizar();

        expect(screen.getByText('Sendera')).toBeInTheDocument();
        expect(await screen.findByLabelText('3 pendientes')).toBeInTheDocument();
        expect(screen.getByRole('link', { name: 'Ver el sitio' })).toHaveAttribute('href', '/');
        expect(screen.getByRole('button', { name: 'Cerrar sesión' })).toBeInTheDocument();
        expect(listarReservasAdmin).toHaveBeenCalledWith({ estado: 'PENDIENTE', pagina: 0, tamano: 1 });
    });
});
