import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { cambiarEstadoReserva, listarReservasAdmin } from '../../services/adminService';
import { ReservaAdmin } from '../../types/admin';
import { EstadoReserva } from '../../types/reserva';
import AdminReservas from './AdminReservas';

vi.mock('../../services/adminService');

const listar = vi.mocked(listarReservasAdmin);

const reserva = (id: number, usuario: string, estado: EstadoReserva): ReservaAdmin => ({
    id,
    usuarioId: 10 + id,
    usuarioNombre: usuario,
    usuarioCorreo: `${usuario.split(' ')[0].toLowerCase()}@correo.com`,
    itinerarioId: 1,
    itinerarioNombre: 'Cocora en dos días',
    destinoNombre: 'Valle de Cocora',
    fechaReserva: '2026-10-16',
    montoTotal: 195000,
    estadoReserva: estado,
});

const pagina = (contenido: ReservaAdmin[], total = contenido.length) => ({ contenido, pagina: 0, tamano: 10, total });

describe('AdminReservas', () => {
    beforeEach(() => {
        listar.mockResolvedValue(
            pagina([
                reserva(1, 'Ana Pérez', 'PENDIENTE'),
                reserva(2, 'Luis Gómez', 'CONFIRMADA'),
                reserva(3, 'Olga Ruiz', 'CANCELADA'),
            ]),
        );
    });

    it('lista las reservas de todos los viajeros con estado, fecha y monto', async () => {
        render(<AdminReservas />);

        expect(await screen.findByText('Ana Pérez')).toBeInTheDocument();
        expect(screen.getByText('Luis Gómez')).toBeInTheDocument();
        expect(screen.getByText('Pendiente')).toBeInTheDocument();
        expect(screen.getByText('Confirmada')).toBeInTheDocument();
        expect(screen.getByText('Cancelada')).toBeInTheDocument();
        expect(screen.getAllByText('16 Oct 2026')).toHaveLength(3);
        expect(screen.getAllByText(/195\.000/)).toHaveLength(3);
    });

    it('ofrece solo las acciones que el estado permite', async () => {
        render(<AdminReservas />);
        await screen.findByText('Ana Pérez');

        // Pendiente: confirmar y cancelar
        expect(screen.getByRole('button', { name: 'Confirmar la reserva 1 de Ana Pérez' })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Cancelar la reserva 1 de Ana Pérez' })).toBeInTheDocument();
        // Confirmada: solo cancelar
        expect(screen.queryByRole('button', { name: /Confirmar la reserva 2/ })).not.toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Cancelar la reserva 2 de Luis Gómez' })).toBeInTheDocument();
        // Cancelada: ninguna
        expect(screen.queryByRole('button', { name: /reserva 3/ })).not.toBeInTheDocument();
        expect(screen.getByText('Sin acciones')).toBeInTheDocument();
    });

    it('pide confirmación y confirma la reserva', async () => {
        vi.mocked(cambiarEstadoReserva).mockResolvedValue(reserva(1, 'Ana Pérez', 'CONFIRMADA'));
        render(<AdminReservas />);

        fireEvent.click(await screen.findByRole('button', { name: 'Confirmar la reserva 1 de Ana Pérez' }));
        const dialogo = screen.getByRole('alertdialog');
        expect(within(dialogo).getByText(/confirmar la reserva n\.º 1 de Ana Pérez/)).toBeInTheDocument();
        expect(cambiarEstadoReserva).not.toHaveBeenCalled();

        fireEvent.click(within(dialogo).getByRole('button', { name: 'Sí, confirmar' }));

        await waitFor(() => expect(cambiarEstadoReserva).toHaveBeenCalledWith(1, 'CONFIRMADA'));
        expect(await screen.findByText('La reserva n.º 1 quedó confirmada.')).toBeInTheDocument();
        expect(listar).toHaveBeenCalledTimes(2);
    });

    it('avisa que cancelar devuelve el cupo y cancela tras confirmar', async () => {
        vi.mocked(cambiarEstadoReserva).mockResolvedValue(reserva(2, 'Luis Gómez', 'CANCELADA'));
        render(<AdminReservas />);

        fireEvent.click(await screen.findByRole('button', { name: 'Cancelar la reserva 2 de Luis Gómez' }));
        const dialogo = screen.getByRole('alertdialog');
        expect(within(dialogo).getByText(/Se devolverá el cupo/)).toBeInTheDocument();
        fireEvent.click(within(dialogo).getByRole('button', { name: 'Sí, cancelar reserva' }));

        await waitFor(() => expect(cambiarEstadoReserva).toHaveBeenCalledWith(2, 'CANCELADA'));
        expect(await screen.findByText('La reserva n.º 2 quedó cancelada y se devolvió el cupo.')).toBeInTheDocument();
    });

    it('cerrar el diálogo no cambia nada', async () => {
        render(<AdminReservas />);

        fireEvent.click(await screen.findByRole('button', { name: 'Cancelar la reserva 1 de Ana Pérez' }));
        fireEvent.click(within(screen.getByRole('alertdialog')).getByRole('button', { name: 'Cancelar' }));

        expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
        expect(cambiarEstadoReserva).not.toHaveBeenCalled();
    });

    it('si el cambio falla, muestra el error y vuelve a consultar el estado real', async () => {
        vi.mocked(cambiarEstadoReserva).mockRejectedValue(new Error('x'));
        render(<AdminReservas />);

        fireEvent.click(await screen.findByRole('button', { name: 'Confirmar la reserva 1 de Ana Pérez' }));
        fireEvent.click(screen.getByRole('button', { name: 'Sí, confirmar' }));

        expect(await screen.findByRole('alert')).toHaveTextContent('No pudimos cambiar el estado de la reserva');
        await waitFor(() => expect(listar).toHaveBeenCalledTimes(2));
    });

    it('busca con todos los filtros y vuelve a la primera página', async () => {
        render(<AdminReservas />);
        await screen.findByText('Ana Pérez');

        fireEvent.change(screen.getByLabelText('Viajero, correo o destino'), { target: { value: 'ana' } });
        fireEvent.change(screen.getByLabelText('Estado'), { target: { value: 'CONFIRMADA' } });
        fireEvent.change(screen.getByLabelText('Reservadas desde'), { target: { value: '2026-10-01' } });
        fireEvent.change(screen.getByLabelText('Reservadas hasta'), { target: { value: '2026-10-31' } });
        fireEvent.click(screen.getByRole('button', { name: 'Buscar' }));

        await waitFor(() =>
            expect(listar).toHaveBeenLastCalledWith({
                estado: 'CONFIRMADA',
                q: 'ana',
                desde: '2026-10-01',
                hasta: '2026-10-31',
                pagina: 0,
                tamano: 10,
            }),
        );
    });

    it('no envía un rango de fechas invertido', async () => {
        render(<AdminReservas />);
        await screen.findByText('Ana Pérez');

        fireEvent.change(screen.getByLabelText('Reservadas desde'), { target: { value: '2026-10-20' } });
        fireEvent.change(screen.getByLabelText('Reservadas hasta'), { target: { value: '2026-10-01' } });
        // El navegador ya bloquea el envío por el atributo min; esto cubre el envío programático.
        fireEvent.submit(screen.getByRole('search', { name: 'Buscar reservas' }));

        expect(screen.getByRole('alert')).toHaveTextContent('La fecha inicial no puede ser posterior a la final.');
        expect(listar).toHaveBeenCalledTimes(1);
    });

    it('limpiar quita los filtros', async () => {
        render(<AdminReservas />);
        await screen.findByText('Ana Pérez');
        fireEvent.change(screen.getByLabelText('Estado'), { target: { value: 'CANCELADA' } });
        fireEvent.click(screen.getByRole('button', { name: 'Buscar' }));
        await waitFor(() => expect(listar).toHaveBeenCalledTimes(2));

        fireEvent.click(screen.getByRole('button', { name: 'Limpiar' }));

        await waitFor(() => expect(listar).toHaveBeenLastCalledWith({ pagina: 0, tamano: 10 }));
        expect(screen.getByLabelText('Estado')).toHaveValue('');
    });

    it('avisa cuando no hay reservas y cuando la carga falla', async () => {
        listar.mockResolvedValueOnce(pagina([]));
        const { unmount } = render(<AdminReservas />);
        expect(await screen.findByText('No encontramos reservas con esos filtros.')).toBeInTheDocument();
        unmount();

        listar.mockRejectedValueOnce(new Error('x'));
        render(<AdminReservas />);
        expect(await screen.findByRole('alert')).toHaveTextContent('No pudimos cargar las reservas');
    });

    it('pagina los resultados', async () => {
        listar.mockResolvedValue({
            contenido: [reserva(1, 'Ana Pérez', 'PENDIENTE')],
            pagina: 0,
            tamano: 10,
            total: 25,
        });
        render(<AdminReservas />);

        expect(await screen.findByText(/Página 1 de 3 · 25 reservas/)).toBeInTheDocument();
        fireEvent.click(screen.getByRole('button', { name: 'Siguiente' }));

        await waitFor(() => expect(listar).toHaveBeenLastCalledWith({ pagina: 1, tamano: 10 }));
    });
});
