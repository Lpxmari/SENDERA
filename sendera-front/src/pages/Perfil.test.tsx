import { fireEvent, render, screen, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it } from 'vitest';
import { AuthProvider } from '../context/AuthContext';
import { reiniciarPerfilSimulado } from '../services/perfilService';
import { guardarSesion } from '../services/sesion';
import Perfil from './Perfil';

const escribir = (etiqueta: string, valor: string) =>
    fireEvent.change(screen.getByLabelText(etiqueta), { target: { value: valor } });

const ANA = { id: 1, nombre: 'Ana', apellidos: 'Perlata Cardenas', correo: 'ana@correo.com' };

const renderizar = (usuario = ANA) => {
    guardarSesion({ token: 'token-de-prueba', usuario });
    return render(
        <MemoryRouter>
            <AuthProvider>
                <Perfil />
            </AuthProvider>
        </MemoryRouter>,
    );
};

describe('Perfil', () => {
    beforeEach(() => reiniciarPerfilSimulado());

    it('muestra los datos de la persona y las opiniones', async () => {
        renderizar();

        expect(await screen.findByText(/Hola, Ana/)).toBeInTheDocument();
        expect(screen.getByText('ana@correo.com')).toBeInTheDocument();
        expect(screen.getByText('23 Mar 1965')).toBeInTheDocument();
        expect(screen.getByText('Algunas de tus opiniones:')).toBeInTheDocument();
        expect(screen.getAllByRole('blockquote')).toHaveLength(3);
    });

    it('edita la información personal y guarda los cambios', async () => {
        renderizar();

        fireEvent.click(await screen.findByRole('button', { name: 'Editar información personal' }));
        escribir('Nombre', 'Ana María');
        fireEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

        expect(await screen.findByText('Tus cambios se guardaron.')).toBeInTheDocument();
        expect(screen.getByText('Ana María')).toBeInTheDocument();
    });

    it('rechaza una identificación inválida sin guardar', async () => {
        renderizar();

        fireEvent.click(await screen.findByRole('button', { name: 'Editar información personal' }));
        escribir('No. Identificación', 'abc');
        fireEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

        expect(screen.getByRole('alert')).toHaveTextContent('entre 5 y 15 dígitos');
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

    it('despliega el historial de viajes', async () => {
        renderizar();

        fireEvent.click(await screen.findByRole('button', { name: 'Consultar Historial de Viajes' }));

        const historial = await screen.findByRole('region', { name: 'Historial de viajes' });
        expect(within(historial).getAllByRole('listitem')).toHaveLength(3);
    });

    it('muestra el perfil de la persona con sesión y no el de otra que entró antes', async () => {
        const MARIA = { id: 2, nombre: 'María', apellidos: 'Ramírez', correo: 'maria@correo.com' };
        const primera = renderizar(ANA);
        expect(await screen.findByText('ana@correo.com')).toBeInTheDocument();
        primera.unmount();

        renderizar(MARIA);

        expect(await screen.findByText('maria@correo.com')).toBeInTheDocument();
        expect(screen.queryByText('ana@correo.com')).not.toBeInTheDocument();
    });
});
