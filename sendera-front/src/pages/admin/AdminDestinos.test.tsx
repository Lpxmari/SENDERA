import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { actualizarDestino, crearDestino, eliminarDestino, listarDestinos } from '../../services/destinoService';
import { Destino } from '../../types/destino';
import AdminDestinos from './AdminDestinos';

vi.mock('../../services/destinoService');

const DESTINO: Destino = {
    id: 5,
    nombre: 'Valle de Cocora',
    descripcion: 'Palmas de cera',
    ubicacion: 'Salento, Quindío',
    precioBase: 85000,
    dificultad: 'MODERADA',
    accesibilidad: true,
    fotosUrl: ['/imagenes/destinos/cocora-1.jpg', '/imagenes/destinos/cocora-2.jpg'],
};

const escribir = (etiqueta: string, valor: string) =>
    fireEvent.change(screen.getByLabelText(etiqueta), { target: { value: valor } });

describe('AdminDestinos', () => {
    beforeEach(() => {
        vi.mocked(listarDestinos).mockResolvedValue([DESTINO]);
    });

    it('lista los destinos con precio y dificultad legibles', async () => {
        render(<AdminDestinos />);

        expect(await screen.findByText('Valle de Cocora')).toBeInTheDocument();
        expect(screen.getByText('Moderada')).toBeInTheDocument();
        expect(screen.getByText('Sí')).toBeInTheDocument();
    });

    it('crea un destino y recarga la lista', async () => {
        vi.mocked(crearDestino).mockResolvedValue({ ...DESTINO, id: 9 });
        render(<AdminDestinos />);
        await screen.findByText('Valle de Cocora');

        fireEvent.click(screen.getByRole('button', { name: 'Nuevo destino' }));
        escribir('Nombre', '  Tayrona ');
        escribir('Ubicación', 'Santa Marta');
        escribir('Descripción', 'Playas y senderos');
        escribir('Precio base (COP)', '145000');
        escribir('Dificultad', 'ALTA');
        fireEvent.click(screen.getByLabelText(/Es accesible/));
        escribir(
            'Fotos (una dirección por línea)',
            '/imagenes/destinos/tayrona-1.jpg\n\n  /imagenes/destinos/tayrona-2.jpg ',
        );
        fireEvent.click(screen.getByRole('button', { name: 'Guardar destino' }));

        await waitFor(() =>
            expect(crearDestino).toHaveBeenCalledWith({
                nombre: 'Tayrona',
                descripcion: 'Playas y senderos',
                ubicacion: 'Santa Marta',
                precioBase: 145000,
                dificultad: 'ALTA',
                accesibilidad: true,
                fotosUrl: ['/imagenes/destinos/tayrona-1.jpg', '/imagenes/destinos/tayrona-2.jpg'],
            }),
        );
        expect(await screen.findByText('El destino se creó correctamente.')).toBeInTheDocument();
        expect(listarDestinos).toHaveBeenCalledTimes(2);
    });

    it('valida los datos antes de enviar', async () => {
        render(<AdminDestinos />);
        await screen.findByText('Valle de Cocora');

        fireEvent.click(screen.getByRole('button', { name: 'Nuevo destino' }));
        fireEvent.click(screen.getByRole('button', { name: 'Guardar destino' }));
        expect(screen.getByRole('alert')).toHaveTextContent('Escribe el nombre del destino.');

        escribir('Nombre', 'Tayrona');
        escribir('Ubicación', 'Santa Marta');
        escribir('Descripción', 'Playas');
        escribir('Precio base (COP)', '-5');
        fireEvent.click(screen.getByRole('button', { name: 'Guardar destino' }));
        expect(screen.getByRole('alert')).toHaveTextContent('El precio base no puede ser negativo.');

        escribir('Precio base (COP)', 'abc');
        fireEvent.click(screen.getByRole('button', { name: 'Guardar destino' }));
        expect(screen.getByRole('alert')).toHaveTextContent('Escribe el precio base como un número.');
        expect(crearDestino).not.toHaveBeenCalled();
    });

    it('edita un destino existente precargando sus datos', async () => {
        vi.mocked(actualizarDestino).mockResolvedValue(DESTINO);
        render(<AdminDestinos />);

        fireEvent.click(await screen.findByRole('button', { name: 'Editar Valle de Cocora' }));
        expect(screen.getByLabelText('Nombre')).toHaveValue('Valle de Cocora');
        expect(screen.getByLabelText('Dificultad')).toHaveValue('MODERADA');
        expect(screen.getByLabelText(/Es accesible/)).toBeChecked();

        escribir('Precio base (COP)', '90000');
        fireEvent.click(screen.getByRole('button', { name: 'Guardar destino' }));

        await waitFor(() =>
            expect(actualizarDestino).toHaveBeenCalledWith(
                5,
                expect.objectContaining({ precioBase: 90000, fotosUrl: DESTINO.fotosUrl }),
            ),
        );
        expect(await screen.findByText('Los cambios del destino se guardaron.')).toBeInTheDocument();
    });

    it('muestra el error del servidor dentro del formulario', async () => {
        vi.mocked(crearDestino).mockRejectedValue(new Error('x'));
        render(<AdminDestinos />);
        await screen.findByText('Valle de Cocora');

        fireEvent.click(screen.getByRole('button', { name: 'Nuevo destino' }));
        escribir('Nombre', 'Tayrona');
        escribir('Ubicación', 'Santa Marta');
        escribir('Descripción', 'Playas');
        escribir('Precio base (COP)', '1000');
        fireEvent.click(screen.getByRole('button', { name: 'Guardar destino' }));

        expect(await screen.findByRole('alert')).toHaveTextContent('No pudimos guardar el destino');
        expect(screen.getByLabelText('Nombre')).toHaveValue('Tayrona');
    });

    it('pide confirmación antes de eliminar y elimina', async () => {
        vi.mocked(eliminarDestino).mockResolvedValue(undefined);
        render(<AdminDestinos />);

        fireEvent.click(await screen.findByRole('button', { name: 'Eliminar Valle de Cocora' }));
        const dialogo = screen.getByRole('alertdialog');
        expect(eliminarDestino).not.toHaveBeenCalled();
        fireEvent.click(within(dialogo).getByRole('button', { name: 'Sí, eliminar' }));

        await waitFor(() => expect(eliminarDestino).toHaveBeenCalledWith(5));
        expect(await screen.findByText('Se eliminó el destino “Valle de Cocora”.')).toBeInTheDocument();
    });

    it('cancelar la eliminación no borra nada', async () => {
        render(<AdminDestinos />);

        fireEvent.click(await screen.findByRole('button', { name: 'Eliminar Valle de Cocora' }));
        fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }));

        expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
        expect(eliminarDestino).not.toHaveBeenCalled();
    });

    it('muestra el aviso del backend si el destino tiene itinerarios', async () => {
        vi.mocked(eliminarDestino).mockRejectedValue(new Error('x'));
        render(<AdminDestinos />);

        fireEvent.click(await screen.findByRole('button', { name: 'Eliminar Valle de Cocora' }));
        fireEvent.click(screen.getByRole('button', { name: 'Sí, eliminar' }));

        expect(await screen.findByRole('alert')).toHaveTextContent('No pudimos eliminar el destino');
    });

    it('indica cuando todavía no hay destinos', async () => {
        vi.mocked(listarDestinos).mockResolvedValue([]);
        render(<AdminDestinos />);

        expect(await screen.findByText('Todavía no hay destinos. Crea el primero.')).toBeInTheDocument();
    });
});
