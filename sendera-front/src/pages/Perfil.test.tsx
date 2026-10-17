import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthProvider } from '../context/AuthContext';
import { actualizarCuenta } from '../services/authService';
import { reiniciarPerfilSimulado } from '../services/perfilService';
import { guardarSesion, leerSesion } from '../services/sesion';
import { ActualizarCuentaRequest, Usuario } from '../types/auth';
import Perfil from './Perfil';

vi.mock('../services/authService', async (original) => ({
    ...(await original<typeof import('../services/authService')>()),
    actualizarCuenta: vi.fn(),
}));

const escribir = (etiqueta: string, valor: string) =>
    fireEvent.change(screen.getByLabelText(etiqueta), { target: { value: valor } });

const ANA: Usuario = { id: 1, nombre: 'Ana', apellidos: 'Perlata Cardenas', correo: 'ana@correo.com' };

const renderizar = (usuario: Usuario = ANA) => {
    guardarSesion({ token: 'token-antiguo', usuario });
    return render(
        <MemoryRouter>
            <AuthProvider>
                <Perfil />
            </AuthProvider>
        </MemoryRouter>,
    );
};

/** El backend devuelve la cuenta ya actualizada y un token nuevo. */
const respuestaBackend = (cuerpo: ActualizarCuentaRequest, usuario: Usuario = ANA) => ({
    token: 'token-nuevo',
    usuario: { ...usuario, nombre: cuerpo.nombre, apellidos: cuerpo.apellidos, correo: cuerpo.correo.toLowerCase() },
});

describe('Perfil', () => {
    beforeEach(() => {
        reiniciarPerfilSimulado();
        vi.mocked(actualizarCuenta).mockImplementation(async (cuerpo) => respuestaBackend(cuerpo));
    });

    it('muestra los datos de la persona y las opiniones', async () => {
        renderizar();

        expect(await screen.findByText(/Hola, Ana/)).toBeInTheDocument();
        expect(screen.getByText('ana@correo.com')).toBeInTheDocument();
        expect(screen.getByText('23 Mar 1965')).toBeInTheDocument();
        expect(screen.getByText('Algunas de tus opiniones:')).toBeInTheDocument();
        expect(screen.getAllByRole('blockquote')).toHaveLength(3);
    });

    it('guarda el nombre en el backend y actualiza la sesión', async () => {
        renderizar();

        fireEvent.click(await screen.findByRole('button', { name: 'Editar información personal' }));
        escribir('Nombre', 'Ana María');
        fireEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

        expect(await screen.findByText('Tus cambios se guardaron.')).toBeInTheDocument();
        expect(screen.getByText('Ana María')).toBeInTheDocument();
        expect(actualizarCuenta).toHaveBeenCalledWith({
            nombre: 'Ana María',
            apellidos: 'Perlata Cardenas',
            correo: 'ana@correo.com',
            contrasenaActual: undefined,
        });
        expect(leerSesion()?.token).toBe('token-nuevo');
        expect(leerSesion()?.usuario.nombre).toBe('Ana María');
    });

    it('rechaza una identificación inválida sin llamar al backend', async () => {
        renderizar();

        fireEvent.click(await screen.findByRole('button', { name: 'Editar información personal' }));
        escribir('No. Identificación', 'abc');
        fireEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

        expect(screen.getByRole('alert')).toHaveTextContent('entre 5 y 15 dígitos');
        expect(actualizarCuenta).not.toHaveBeenCalled();
    });

    it('valida el correo en la información de viajero y permite cancelar', async () => {
        renderizar();

        fireEvent.click(await screen.findByRole('button', { name: 'Editar información de viajero' }));
        escribir('Correo', 'no-es-correo');
        fireEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));
        expect(screen.getByRole('alert')).toHaveTextContent('correo válido');

        fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }));
        expect(screen.getByText('ana@correo.com')).toBeInTheDocument();
    });

    it('pide la contraseña actual solo cuando el correo cambia', async () => {
        renderizar();

        fireEvent.click(await screen.findByRole('button', { name: 'Editar información de viajero' }));
        expect(screen.queryByLabelText(/Contraseña actual/)).not.toBeInTheDocument();

        escribir('Correo', 'mariana@correo.com');
        expect(screen.getByLabelText(/Contraseña actual/)).toBeInTheDocument();

        escribir('Correo', 'ANA@correo.com');
        expect(screen.queryByLabelText(/Contraseña actual/)).not.toBeInTheDocument();
    });

    it('no guarda un correo nuevo sin la contraseña actual', async () => {
        renderizar();

        fireEvent.click(await screen.findByRole('button', { name: 'Editar información de viajero' }));
        escribir('Correo', 'mariana@correo.com');
        fireEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

        expect(screen.getByRole('alert')).toHaveTextContent('escribe tu contraseña actual');
        expect(actualizarCuenta).not.toHaveBeenCalled();
    });

    it('cambia el correo en el backend y deja la sesión con el correo y el token nuevos', async () => {
        renderizar();

        fireEvent.click(await screen.findByRole('button', { name: 'Editar información de viajero' }));
        escribir('Correo', '  Mariana@Correo.com ');
        escribir('Contraseña actual (necesaria para cambiar tu correo)', 'clave1234');
        fireEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

        expect(await screen.findByText('Tus cambios se guardaron.')).toBeInTheDocument();
        expect(actualizarCuenta).toHaveBeenCalledWith({
            nombre: 'Ana',
            apellidos: 'Perlata Cardenas',
            correo: 'Mariana@Correo.com',
            contrasenaActual: 'clave1234',
        });
        expect(screen.getByText('mariana@correo.com')).toBeInTheDocument();
        expect(leerSesion()?.usuario.correo).toBe('mariana@correo.com');
        expect(leerSesion()?.token).toBe('token-nuevo');
    });

    it('si el backend rechaza el cambio, muestra el error y conserva lo escrito', async () => {
        vi.mocked(actualizarCuenta).mockRejectedValue(new Error('x'));
        renderizar();

        fireEvent.click(await screen.findByRole('button', { name: 'Editar información de viajero' }));
        escribir('Correo', 'mariana@correo.com');
        escribir('Contraseña actual (necesaria para cambiar tu correo)', 'incorrecta1');
        fireEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

        expect(await screen.findByRole('alert')).toHaveTextContent('No pudimos guardar tus cambios');
        expect(screen.getByLabelText('Correo')).toHaveValue('mariana@correo.com');
        expect(leerSesion()?.token).toBe('token-antiguo');
        expect(leerSesion()?.usuario.correo).toBe('ana@correo.com');
    });

    it('despliega el historial de viajes', async () => {
        renderizar();

        fireEvent.click(await screen.findByRole('button', { name: 'Consultar Historial de Viajes' }));

        const historial = await screen.findByRole('region', { name: 'Historial de viajes' });
        expect(within(historial).getAllByRole('listitem')).toHaveLength(3);
    });

    it('muestra el perfil de la persona con sesión y no el de otra que entró antes', async () => {
        const MARIA: Usuario = { id: 2, nombre: 'María', apellidos: 'Ramírez', correo: 'maria@correo.com' };
        const primera = renderizar(ANA);
        expect(await screen.findByText('ana@correo.com')).toBeInTheDocument();
        primera.unmount();

        renderizar(MARIA);

        expect(await screen.findByText('maria@correo.com')).toBeInTheDocument();
        expect(screen.queryByText('ana@correo.com')).not.toBeInTheDocument();
    });

    it('al recargar, los datos reales vienen de la sesión y no de la memoria anterior', async () => {
        const primera = renderizar();
        fireEvent.click(await screen.findByRole('button', { name: 'Editar información personal' }));
        escribir('Nombre', 'Ana María');
        fireEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));
        await screen.findByText('Tus cambios se guardaron.');
        primera.unmount();

        // Otra sesión con el nombre real guardado en el backend: manda la sesión.
        renderizar({ ...ANA, nombre: 'Ana Lucía' });

        await waitFor(() => expect(screen.getByText('Ana Lucía')).toBeInTheDocument());
        expect(screen.queryByText('Ana María')).not.toBeInTheDocument();
    });
});
