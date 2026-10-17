import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { eliminarResenaAdmin, listarResenasAdmin } from '../../services/adminService';
import { listarDestinos } from '../../services/destinoService';
import { ResenaAdmin } from '../../types/admin';
import AdminResenas from './AdminResenas';

vi.mock('../../services/adminService');
vi.mock('../../services/destinoService');

const listar = vi.mocked(listarResenasAdmin);

const resena = (
    id: number,
    autor: string,
    calificacion: number,
    tieneImagen = false,
    comentario = 'Muy bonito',
): ResenaAdmin => ({
    id,
    destinoId: 5,
    destinoNombre: 'Valle de Cocora',
    usuarioId: 20 + id,
    autor,
    comentario,
    calificacion,
    tieneImagen,
    creadaEn: '2026-10-15T08:00:00',
});

const pagina = (contenido: ResenaAdmin[], total = contenido.length) => ({ contenido, pagina: 0, tamano: 10, total });

describe('AdminResenas', () => {
    beforeEach(() => {
        listar.mockResolvedValue(
            pagina([resena(1, 'Ana Pérez', 5, true), resena(2, 'Luis Gómez', 1, false, 'Estaba sucio y mal atendido')]),
        );
        vi.mocked(listarDestinos).mockResolvedValue([
            {
                id: 5,
                nombre: 'Valle de Cocora',
                descripcion: 'x',
                ubicacion: 'Salento',
                precioBase: 85000,
                dificultad: 'BAJA',
                accesibilidad: true,
                fotosUrl: [],
            },
        ]);
    });

    it('lista las reseñas con calificación accesible, imagen y fecha', async () => {
        render(<AdminResenas />);

        expect(await screen.findByText('Ana Pérez')).toBeInTheDocument();
        expect(screen.getByText('5 de 5 estrellas')).toBeInTheDocument();
        expect(screen.getByText('1 de 5 estrellas')).toBeInTheDocument();
        expect(screen.getByRole('link', { name: 'Ver la imagen de la reseña de Ana Pérez' })).toHaveAttribute(
            'href',
            '/api/v1/destinos/5/resenas/1/imagen',
        );
        expect(screen.queryByRole('link', { name: /Luis Gómez/ })).not.toBeInTheDocument();
        expect(screen.getAllByText('15 Oct 2026')).toHaveLength(2);
    });

    it('muestra el comentario completo, aunque sea largo, para poder moderarlo', async () => {
        const largo = `${'a'.repeat(300)} fin`;
        listar.mockResolvedValue(pagina([resena(1, 'Ana Pérez', 4, false, largo)]));
        render(<AdminResenas />);

        expect(await screen.findByText(largo)).toBeInTheDocument();
    });

    it('resalta las reseñas con calificación baja', async () => {
        render(<AdminResenas />);

        const items = await screen.findAllByRole('listitem');
        expect(items[0]).not.toHaveClass('admin-resena--baja');
        expect(items[1]).toHaveClass('admin-resena--baja');
    });

    it('carga los destinos en el filtro', async () => {
        render(<AdminResenas />);

        expect(await screen.findByRole('option', { name: 'Valle de Cocora' })).toBeInTheDocument();
    });

    it('busca por destino, calificación y texto', async () => {
        render(<AdminResenas />);
        await screen.findByText('Ana Pérez');
        await screen.findByRole('option', { name: 'Valle de Cocora' });

        fireEvent.change(screen.getByLabelText('Destino'), { target: { value: '5' } });
        fireEvent.change(screen.getByLabelText('Calificación'), { target: { value: '2' } });
        fireEvent.change(screen.getByLabelText('Comentario o autor'), { target: { value: 'sucio' } });
        fireEvent.click(screen.getByRole('button', { name: 'Buscar' }));

        await waitFor(() =>
            expect(listar).toHaveBeenLastCalledWith({
                destinoId: 5,
                calificacionMax: 2,
                q: 'sucio',
                pagina: 0,
                tamano: 10,
            }),
        );
    });

    it('pide confirmación y elimina la reseña', async () => {
        vi.mocked(eliminarResenaAdmin).mockResolvedValue(undefined);
        render(<AdminResenas />);

        fireEvent.click(
            await screen.findByRole('button', { name: 'Eliminar la reseña de Luis Gómez sobre Valle de Cocora' }),
        );
        const dialogo = screen.getByRole('alertdialog');
        expect(within(dialogo).getByText(/reseña de Luis Gómez/)).toBeInTheDocument();
        expect(eliminarResenaAdmin).not.toHaveBeenCalled();

        fireEvent.click(within(dialogo).getByRole('button', { name: 'Sí, eliminar' }));

        await waitFor(() => expect(eliminarResenaAdmin).toHaveBeenCalledWith(2));
        expect(await screen.findByText('Se eliminó la reseña de Luis Gómez.')).toBeInTheDocument();
        expect(listar).toHaveBeenCalledTimes(2);
    });

    it('cancelar la eliminación no borra nada', async () => {
        render(<AdminResenas />);

        fireEvent.click(
            await screen.findByRole('button', { name: 'Eliminar la reseña de Ana Pérez sobre Valle de Cocora' }),
        );
        fireEvent.click(within(screen.getByRole('alertdialog')).getByRole('button', { name: 'Cancelar' }));

        expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
        expect(eliminarResenaAdmin).not.toHaveBeenCalled();
    });

    it('muestra el error si la eliminación falla', async () => {
        vi.mocked(eliminarResenaAdmin).mockRejectedValue(new Error('x'));
        render(<AdminResenas />);

        fireEvent.click(
            await screen.findByRole('button', { name: 'Eliminar la reseña de Ana Pérez sobre Valle de Cocora' }),
        );
        fireEvent.click(screen.getByRole('button', { name: 'Sí, eliminar' }));

        expect(await screen.findByRole('alert')).toHaveTextContent('No pudimos eliminar la reseña');
    });

    it('avisa cuando no hay reseñas y cuando la carga falla', async () => {
        listar.mockResolvedValueOnce(pagina([]));
        const { unmount } = render(<AdminResenas />);
        expect(await screen.findByText('No encontramos reseñas con esos filtros.')).toBeInTheDocument();
        unmount();

        listar.mockRejectedValueOnce(new Error('x'));
        render(<AdminResenas />);
        expect(await screen.findByRole('alert')).toHaveTextContent('No pudimos cargar las reseñas');
    });

    it('pagina los resultados', async () => {
        listar.mockResolvedValue({ contenido: [resena(1, 'Ana Pérez', 5)], pagina: 0, tamano: 10, total: 11 });
        render(<AdminResenas />);

        expect(await screen.findByText(/Página 1 de 2 · 11 reseñas/)).toBeInTheDocument();
        fireEvent.click(screen.getByRole('button', { name: 'Siguiente' }));

        await waitFor(() => expect(listar).toHaveBeenLastCalledWith({ pagina: 1, tamano: 10 }));
    });
});
