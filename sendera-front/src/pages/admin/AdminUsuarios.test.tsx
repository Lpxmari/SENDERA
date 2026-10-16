import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthProvider } from '../../context/AuthContext';
import { cambiarActivoUsuario, cambiarRolUsuario, listarUsuariosAdmin } from '../../services/adminService';
import { guardarSesion } from '../../services/sesion';
import { UsuarioAdmin } from '../../types/admin';
import AdminUsuarios from './AdminUsuarios';

vi.mock('../../services/adminService');

const listar = vi.mocked(listarUsuariosAdmin);

const usuario = (
    id: number,
    nombre: string,
    rol: UsuarioAdmin['rol'] = 'ROLE_CLIENTE',
    activo = true,
): UsuarioAdmin => ({
    id,
    nombre,
    apellidos: 'Pérez',
    correo: `${nombre.toLowerCase()}@correo.com`,
    rol,
    activo,
    fechaRegistro: '2026-10-16T09:30:00',
});

const pagina = (contenido: UsuarioAdmin[], total = contenido.length) => ({ contenido, pagina: 0, tamano: 10, total });

const renderizar = () => {
    guardarSesion({
        token: 'token-de-prueba',
        usuario: { id: 1, nombre: 'Ada', apellidos: 'Pérez', correo: 'ada@correo.com', rol: 'ROLE_ADMIN' },
    });
    return render(
        <MemoryRouter>
            <AuthProvider>
                <AdminUsuarios />
            </AuthProvider>
        </MemoryRouter>,
    );
};

describe('AdminUsuarios', () => {
    beforeEach(() => {
        listar.mockResolvedValue(
            pagina([usuario(1, 'Ada', 'ROLE_ADMIN'), usuario(2, 'Carlos'), usuario(3, 'Olga', 'ROLE_CLIENTE', false)]),
        );
    });

    it('muestra los usuarios con su rol, estado y fecha', async () => {
        renderizar();

        expect(await screen.findByText('Carlos Pérez')).toBeInTheDocument();
        expect(screen.getAllByText('Activo')).toHaveLength(2);
        expect(screen.getByText('Desactivado')).toBeInTheDocument();
        expect(screen.getAllByText('16 Oct 2026')).toHaveLength(3);
    });

    it('no ofrece acciones sobre la propia cuenta', async () => {
        renderizar();

        await screen.findByText('Carlos Pérez');
        expect(screen.getByText('(tú)')).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /Desactivar la cuenta de Ada/ })).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /administrador a Ada/ })).not.toBeInTheDocument();
    });

    it('pide confirmación y desactiva la cuenta', async () => {
        vi.mocked(cambiarActivoUsuario).mockResolvedValue(usuario(2, 'Carlos', 'ROLE_CLIENTE', false));
        renderizar();

        fireEvent.click(await screen.findByRole('button', { name: 'Desactivar la cuenta de Carlos Pérez' }));
        const dialogo = screen.getByRole('alertdialog');
        expect(within(dialogo).getByText(/quitarle el acceso a Carlos Pérez/)).toBeInTheDocument();
        expect(cambiarActivoUsuario).not.toHaveBeenCalled();

        fireEvent.click(within(dialogo).getByRole('button', { name: 'Sí, desactivar' }));

        await waitFor(() => expect(cambiarActivoUsuario).toHaveBeenCalledWith(2, false));
        expect(await screen.findByText('La cuenta de Carlos Pérez quedó desactivada.')).toBeInTheDocument();
        expect(listar).toHaveBeenCalledTimes(2);
    });

    it('cancelar el diálogo no cambia nada', async () => {
        renderizar();

        fireEvent.click(await screen.findByRole('button', { name: 'Hacer administrador a Carlos Pérez' }));
        fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }));

        expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
        expect(cambiarRolUsuario).not.toHaveBeenCalled();
    });

    it('cambia el rol tras confirmar', async () => {
        vi.mocked(cambiarRolUsuario).mockResolvedValue(usuario(2, 'Carlos', 'ROLE_ADMIN'));
        renderizar();

        fireEvent.click(await screen.findByRole('button', { name: 'Hacer administrador a Carlos Pérez' }));
        fireEvent.click(screen.getByRole('button', { name: 'Sí, hacer administrador' }));

        await waitFor(() => expect(cambiarRolUsuario).toHaveBeenCalledWith(2, 'ROLE_ADMIN'));
        expect(await screen.findByText('Carlos Pérez ahora es administrador.')).toBeInTheDocument();
    });

    it('muestra el mensaje del backend cuando la acción falla', async () => {
        vi.mocked(cambiarActivoUsuario).mockRejectedValue(new Error('x'));
        renderizar();

        fireEvent.click(await screen.findByRole('button', { name: 'Desactivar la cuenta de Carlos Pérez' }));
        fireEvent.click(screen.getByRole('button', { name: 'Sí, desactivar' }));

        expect(await screen.findByRole('alert')).toHaveTextContent('No pudimos guardar el cambio');
    });

    it('busca con los filtros y vuelve a la primera página', async () => {
        renderizar();
        await screen.findByText('Carlos Pérez');

        fireEvent.change(screen.getByLabelText('Nombre o correo'), { target: { value: 'carlos' } });
        fireEvent.change(screen.getByLabelText('Estado'), { target: { value: 'false' } });
        fireEvent.click(screen.getByRole('button', { name: 'Buscar' }));

        await waitFor(() =>
            expect(listar).toHaveBeenLastCalledWith({ q: 'carlos', rol: '', activo: false, pagina: 0, tamano: 10 }),
        );
    });

    it('avisa cuando no hay resultados', async () => {
        listar.mockResolvedValue(pagina([]));
        renderizar();

        expect(await screen.findByText('No encontramos usuarios con esos filtros.')).toBeInTheDocument();
    });

    it('muestra un error claro si la carga falla', async () => {
        listar.mockRejectedValue(new Error('x'));
        renderizar();

        expect(await screen.findByRole('alert')).toHaveTextContent('No pudimos cargar los usuarios');
    });

    it('pagina los resultados', async () => {
        listar.mockResolvedValue({ contenido: [usuario(2, 'Carlos')], pagina: 0, tamano: 10, total: 25 });
        renderizar();

        expect(await screen.findByText(/Página 1 de 3 · 25 usuarios/)).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Anterior' })).toBeDisabled();

        fireEvent.click(screen.getByRole('button', { name: 'Siguiente' }));

        await waitFor(() => expect(listar).toHaveBeenLastCalledWith({ pagina: 1, tamano: 10 }));
    });
});
