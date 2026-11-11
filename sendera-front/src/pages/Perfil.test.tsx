import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthProvider } from '../context/AuthContext';
import { actualizarCuenta } from '../services/authService';
import { cancelarReserva, listarMisReservas } from '../services/reservaService';
import { Reserva } from '../types/reserva';
import { reiniciarPerfilSimulado } from '../services/perfilService';
import { guardarSesion, leerSesion } from '../services/sesion';
import { ActualizarCuentaRequest, Usuario } from '../types/auth';
import Perfil from './Perfil';

vi.mock('../services/authService', async (original) => ({
    ...(await original<typeof import('../services/authService')>()),
    actualizarCuenta: vi.fn(),
}));

vi.mock('../services/reservaService', () => ({
    listarMisReservas: vi.fn(),
    crearReserva: vi.fn(),
    iniciarPago: vi.fn(),
    cancelarReserva: vi.fn(),
}));

const reserva = (
    id: number,
    destinoNombre: string,
    fechaReserva: string,
    montoTotal: number,
    estadoReserva: Reserva['estadoReserva'],
): Reserva => ({
    id,
    itinerarioId: id + 10,
    fechaReserva,
    montoTotal,
    estadoReserva,
    destinoNombre,
});

const RESERVAS: Reserva[] = [
    reserva(1, 'Hotel Boutique Casa Aurora', '2026-05-03', 980000, 'CONFIRMADA'),
    reserva(2, 'Finca Hotel Los Arrayanes', '2026-08-12', 450000, 'PENDIENTE'),
    reserva(3, 'Cabañas Playa Dorada', '2026-02-20', 620000, 'CANCELADA'),
];

const escribir = (etiqueta: string, valor: string) =>
    fireEvent.change(screen.getByLabelText(etiqueta), { target: { value: valor } });

const ANA: Usuario = { id: 1, nombre: 'Ana', apellidos: 'Perlata Cardenas', correo: 'ana@correo.com' };

const renderizar = (usuario: Usuario = ANA, ruta = '/perfil') => {
    guardarSesion({ token: 'token-antiguo', usuario });
    return render(
        <MemoryRouter initialEntries={[ruta]}>
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
        vi.mocked(listarMisReservas).mockResolvedValue(RESERVAS);
        vi.mocked(cancelarReserva).mockReset();
    });

    it('muestra los datos de la persona y las opiniones', async () => {
        renderizar();

        expect(await screen.findByText(/Hola, Ana/)).toBeInTheDocument();
        expect(screen.getByText('ana@correo.com')).toBeInTheDocument();
        expect(screen.getByText('23 Mar 1965')).toBeInTheDocument();
        expect(screen.getByText('Algunas de tus opiniones:')).toBeInTheDocument();
        expect(screen.getAllByRole('blockquote')).toHaveLength(3);
    });

    it('ofrece un enlace a «Mis itinerarios»', async () => {
        renderizar();

        const enlace = await screen.findByRole('link', { name: 'Mis itinerarios' });
        expect(enlace).toHaveAttribute('href', '/itinerarios');
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

    it('despliega el historial con las reservas reales, de la más reciente a la más antigua', async () => {
        renderizar();

        fireEvent.click(await screen.findByRole('button', { name: 'Consultar Historial de Viajes' }));

        const historial = await screen.findByRole('region', { name: 'Historial de viajes' });
        const viajes = within(historial).getAllByRole('listitem');
        expect(viajes).toHaveLength(3);
        expect(listarMisReservas).toHaveBeenCalledTimes(1);
        expect(viajes[0]).toHaveTextContent('Finca Hotel Los Arrayanes');
        expect(viajes[1]).toHaveTextContent('Hotel Boutique Casa Aurora');
        expect(viajes[2]).toHaveTextContent('Cabañas Playa Dorada');
        expect(viajes[1]).toHaveTextContent('980.000');
    });

    it('muestra el estado de cada reserva con texto, no solo con color', async () => {
        renderizar();

        fireEvent.click(await screen.findByRole('button', { name: 'Consultar Historial de Viajes' }));

        const historial = await screen.findByRole('region', { name: 'Historial de viajes' });
        expect(within(historial).getByText(/Pendiente de confirmación/)).toBeInTheDocument();
        expect(within(historial).getByText(/Confirmada/)).toBeInTheDocument();
        expect(within(historial).getByText(/Cancelada/)).toBeInTheDocument();
    });

    it('ofrece pagar solo las reservas pendientes', async () => {
        renderizar();

        fireEvent.click(await screen.findByRole('button', { name: 'Consultar Historial de Viajes' }));

        const historial = await screen.findByRole('region', { name: 'Historial de viajes' });
        const botones = within(historial).getAllByRole('button', { name: /^Pagar la reserva/ });
        expect(botones).toHaveLength(1);
        expect(botones[0]).toHaveAccessibleName('Pagar la reserva de Finca Hotel Los Arrayanes');
        // Sin un pago iniciado no hay nada que verificar
        expect(within(historial).queryByRole('link', { name: /Verificar el pago/ })).not.toBeInTheDocument();
    });

    it('con un pago iniciado enlaza a verificarlo', async () => {
        vi.mocked(listarMisReservas).mockResolvedValue([{ ...RESERVAS[1], estadoPago: 'RECHAZADO' }]);
        renderizar();

        fireEvent.click(await screen.findByRole('button', { name: 'Consultar Historial de Viajes' }));

        const historial = await screen.findByRole('region', { name: 'Historial de viajes' });
        expect(
            within(historial).getByRole('link', {
                name: 'Verificar el pago de la reserva de Finca Hotel Los Arrayanes',
            }),
        ).toHaveAttribute('href', '/pago/resultado?reserva=2');
    });

    it('sin reservas muestra un mensaje de lista vacía', async () => {
        vi.mocked(listarMisReservas).mockResolvedValue([]);
        renderizar();

        fireEvent.click(await screen.findByRole('button', { name: 'Consultar Historial de Viajes' }));

        expect(await screen.findByText('Aún no tienes viajes reservados.')).toBeInTheDocument();
    });

    it('si falla la carga del historial muestra un error y no un historial inventado', async () => {
        vi.mocked(listarMisReservas).mockRejectedValue(new Error('red'));
        renderizar();

        fireEvent.click(await screen.findByRole('button', { name: 'Consultar Historial de Viajes' }));

        expect(await screen.findByRole('alert')).toHaveTextContent('No pudimos cargar tu historial de viajes.');
        expect(screen.queryByRole('region', { name: 'Historial de viajes' })).not.toBeInTheDocument();
    });

    it('con ?seccion=reservas abre el historial sin pulsar el botón', async () => {
        renderizar(ANA, '/perfil?seccion=reservas');

        const historial = await screen.findByRole('region', { name: 'Historial de viajes' });
        expect(within(historial).getAllByRole('listitem')).toHaveLength(3);
    });

    it('sin ?seccion=reservas no consulta las reservas hasta que se pide', async () => {
        renderizar();

        await screen.findByText(/Hola, Ana/);
        expect(listarMisReservas).not.toHaveBeenCalled();
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

    describe('cancelar reserva (CP-009 / RNF-30)', () => {
        const abrirHistorial = async () => {
            renderizar();
            fireEvent.click(await screen.findByRole('button', { name: 'Consultar Historial de Viajes' }));
            return screen.findByRole('region', { name: 'Historial de viajes' });
        };

        const confirmar = () =>
            fireEvent.click(within(screen.getByRole('alertdialog')).getByRole('button', { name: 'Sí, cancelar reserva' }));

        it('muestra «Cancelar reserva» solo en reservas pendientes o confirmadas', async () => {
            const historial = await abrirHistorial();

            const botones = within(historial).getAllByRole('button', { name: /^Cancelar la reserva/ });
            expect(botones).toHaveLength(2);
            expect(botones[0]).toHaveAccessibleName('Cancelar la reserva a Finca Hotel Los Arrayanes del 12 Ago 2026');
            expect(botones[1]).toHaveAccessibleName('Cancelar la reserva a Hotel Boutique Casa Aurora del 3 May 2026');
            expect(botones[0]).toHaveTextContent('Cancelar reserva');
            expect(within(historial).queryByRole('button', { name: /Cabañas Playa Dorada/ })).not.toBeInTheDocument();
        });

        it('pide confirmación con el destino y la fecha antes de cancelar', async () => {
            const historial = await abrirHistorial();

            fireEvent.click(within(historial).getByRole('button', { name: /^Cancelar la reserva a Hotel Boutique/ }));

            expect(screen.getByRole('alertdialog')).toHaveTextContent(
                '¿Seguro que quieres cancelar tu reserva a Hotel Boutique Casa Aurora del 3 May 2026? Se liberará tu cupo.',
            );
            expect(cancelarReserva).not.toHaveBeenCalled();
        });

        it('al confirmar llama al servicio, pasa a CANCELADA sin recargar y avisa', async () => {
            vi.mocked(cancelarReserva).mockResolvedValue({ ...RESERVAS[0], estadoReserva: 'CANCELADA' });
            const historial = await abrirHistorial();

            fireEvent.click(within(historial).getByRole('button', { name: /^Cancelar la reserva a Hotel Boutique/ }));
            confirmar();

            expect(await within(historial).findByText(/fue cancelada y se liberó tu cupo/)).toBeInTheDocument();
            expect(cancelarReserva).toHaveBeenCalledWith(1);
            expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
            const fila = within(historial).getByText('Hotel Boutique Casa Aurora').closest('li') as HTMLElement;
            expect(within(fila).getByText(/Cancelada/)).toBeInTheDocument();
            expect(within(fila).queryByRole('button', { name: /^Cancelar la reserva/ })).not.toBeInTheDocument();
            // No se volvió a pedir el historial
            expect(listarMisReservas).toHaveBeenCalledTimes(1);
        });

        it('si se cierra el diálogo no llama al servicio y devuelve el foco al botón', async () => {
            const historial = await abrirHistorial();
            const boton = within(historial).getByRole('button', { name: /^Cancelar la reserva a Hotel Boutique/ });

            fireEvent.click(boton);
            fireEvent.click(within(screen.getByRole('alertdialog')).getByRole('button', { name: 'Cancelar' }));

            expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
            expect(cancelarReserva).not.toHaveBeenCalled();
            await waitFor(() => expect(boton).toHaveFocus());
            expect(within(historial).getAllByRole('button', { name: /^Cancelar la reserva/ })).toHaveLength(2);
        });

        it('si el servidor rechaza, muestra su mensaje y la reserva no cambia', async () => {
            vi.mocked(cancelarReserva).mockRejectedValue(
                Object.assign(new Error('409'), {
                    isAxiosError: true,
                    response: { status: 409, data: { message: 'La reserva ya está cancelada.' } },
                }),
            );
            const historial = await abrirHistorial();

            fireEvent.click(within(historial).getByRole('button', { name: /^Cancelar la reserva a Hotel Boutique/ }));
            confirmar();

            expect(await within(historial).findByRole('alert')).toHaveTextContent('La reserva ya está cancelada.');
            const fila = within(historial).getByText('Hotel Boutique Casa Aurora').closest('li') as HTMLElement;
            expect(within(fila).getByText(/Confirmada/)).toBeInTheDocument();
            expect(within(fila).getByRole('button', { name: /^Cancelar la reserva/ })).toBeInTheDocument();
        });

        it('con un error sin mensaje del servidor usa uno genérico', async () => {
            vi.mocked(cancelarReserva).mockRejectedValue(new Error('x'));
            const historial = await abrirHistorial();

            fireEvent.click(within(historial).getByRole('button', { name: /^Cancelar la reserva a Hotel Boutique/ }));
            confirmar();

            expect(await within(historial).findByRole('alert')).toHaveTextContent('No pudimos cancelar tu reserva.');
        });

        it('mientras cancela bloquea el diálogo para evitar doble envío', async () => {
            let terminar: (r: Reserva) => void = () => undefined;
            vi.mocked(cancelarReserva).mockReturnValue(new Promise<Reserva>((resolver) => (terminar = resolver)));
            const historial = await abrirHistorial();

            fireEvent.click(within(historial).getByRole('button', { name: /^Cancelar la reserva a Hotel Boutique/ }));
            const dialogo = within(screen.getByRole('alertdialog'));
            fireEvent.click(dialogo.getByRole('button', { name: 'Sí, cancelar reserva' }));

            expect(await dialogo.findByRole('button', { name: 'Guardando…' })).toBeDisabled();
            expect(dialogo.getByRole('button', { name: 'Cancelar' })).toBeDisabled();
            terminar({ ...RESERVAS[0], estadoReserva: 'CANCELADA' });
            await waitFor(() => expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument());
            expect(cancelarReserva).toHaveBeenCalledTimes(1);
        });
    });
});
