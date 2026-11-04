import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { Actividad, DuracionActividad } from '../../types/viaje';
import AgregarActividad from './AgregarActividad';

const actividad = (id: number, nombre: string, duracionMinutos: number): Actividad => ({
    id,
    destinoId: 10,
    destinoNombre: 'Filandia',
    ubicacion: 'Filandia, Quindío',
    nombre,
    descripcion: 'Descripción',
    duracionMinutos,
    dificultad: 'BAJA',
    precioPorPersona: 50000,
    accesibilidad: true,
});

const catalogo = [actividad(1, 'Paseo corto', 45), actividad(2, 'Recorrido', 150), actividad(3, 'Caminata', 195)];

const renderizar = (props: Partial<React.ComponentProps<typeof AgregarActividad>> = {}) => {
    const onCambiarDuracion = vi.fn();
    const onAgregar = vi.fn();
    render(
        <AgregarActividad
            catalogo={catalogo}
            nombreDia="Día 1"
            deshabilitado={false}
            onCambiarDuracion={onCambiarDuracion}
            onAgregar={onAgregar}
            {...props}
        />,
    );
    fireEvent.click(screen.getByRole('button', { name: /Agregar actividad al Día 1/ }));
    return { onCambiarDuracion, onAgregar };
};

describe('AgregarActividad — duración (RNF-26)', () => {
    it('muestra la duración legible en cada opción y en el detalle', () => {
        renderizar();

        expect(screen.getByRole('option', { name: 'Paseo corto (45 min)' })).toBeInTheDocument();
        expect(screen.getByRole('option', { name: 'Recorrido (2 h 30 min)' })).toBeInTheDocument();
        expect(screen.getByRole('option', { name: 'Caminata (3 h 15 min)' })).toBeInTheDocument();
        expect(screen.getByText(/Filandia, Quindío · 45 min · Dificultad baja/)).toBeInTheDocument();
    });

    it('ofrece cuatro botones de filtro y marca el activo con aria-pressed', () => {
        renderizar({ duracion: 'DE_61_A_180_MIN' });

        const activos = screen
            .getAllByRole('button', { pressed: true })
            .map((boton) => boton.textContent);
        expect(activos).toEqual(['De 1 a 3 horas']);
        for (const texto of ['Todas', 'Hasta 1 hora', 'Más de 3 horas']) {
            expect(screen.getByRole('button', { name: texto })).toHaveAttribute('aria-pressed', 'false');
        }
    });

    it('sin filtro marca «Todas»', () => {
        renderizar();

        expect(screen.getByRole('button', { name: 'Todas' })).toHaveAttribute('aria-pressed', 'true');
    });

    it.each<[string, DuracionActividad | undefined]>([
        ['Todas', undefined],
        ['Hasta 1 hora', 'HASTA_60_MIN'],
        ['De 1 a 3 horas', 'DE_61_A_180_MIN'],
        ['Más de 3 horas', 'MAS_DE_180_MIN'],
    ])('«%s» pide el rango %s', (texto, esperado) => {
        const { onCambiarDuracion } = renderizar({ duracion: texto === 'Todas' ? 'HASTA_60_MIN' : undefined });

        fireEvent.click(screen.getByRole('button', { name: texto }));

        expect(onCambiarDuracion).toHaveBeenCalledWith(esperado);
    });

    it('sin actividades con esa duración avisa, deshabilita «Agregar» y permite cambiar el filtro', () => {
        const { onCambiarDuracion } = renderizar({ catalogo: [], duracion: 'MAS_DE_180_MIN' });

        expect(screen.getByText('No hay actividades con esa duración. Prueba con otra opción.')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Agregar' })).toBeDisabled();
        fireEvent.click(screen.getByRole('button', { name: 'Todas' }));
        expect(onCambiarDuracion).toHaveBeenCalledWith(undefined);
    });

    it('con el filtro activo y sin resultados, el botón para abrir sigue habilitado', () => {
        render(
            <AgregarActividad
                catalogo={[]}
                nombreDia="Día 1"
                deshabilitado={false}
                duracion="HASTA_60_MIN"
                onCambiarDuracion={vi.fn()}
                onAgregar={vi.fn()}
            />,
        );

        expect(screen.getByRole('button', { name: /Agregar actividad al Día 1/ })).toBeEnabled();
    });

    it('sin filtro y sin catálogo, el botón para abrir queda deshabilitado', () => {
        render(<AgregarActividad catalogo={[]} nombreDia="Día 1" deshabilitado={false} onCambiarDuracion={vi.fn()} onAgregar={vi.fn()} />);

        expect(screen.getByRole('button', { name: /Agregar actividad al Día 1/ })).toBeDisabled();
    });

    it('muestra el estado de búsqueda y el error del filtro', () => {
        renderizar({ filtrando: true, errorFiltro: 'No se pudo aplicar el filtro de duración. Intenta de nuevo.' });

        expect(screen.getByText('Buscando actividades…')).toBeInTheDocument();
        expect(screen.getByRole('alert')).toHaveTextContent('No se pudo aplicar el filtro de duración');
    });

    it('al cambiar el catálogo reinicia la selección a la primera actividad disponible', () => {
        const onAgregar = vi.fn();
        const props = { nombreDia: 'Día 1', deshabilitado: false, onCambiarDuracion: vi.fn(), onAgregar };
        const { rerender } = render(<AgregarActividad catalogo={catalogo} {...props} />);
        fireEvent.click(screen.getByRole('button', { name: /Agregar actividad al Día 1/ }));
        fireEvent.change(screen.getByLabelText('Actividad'), { target: { value: '3' } });

        rerender(<AgregarActividad catalogo={[catalogo[0]]} {...props} />);
        fireEvent.click(screen.getByRole('button', { name: 'Agregar' }));

        expect(onAgregar).toHaveBeenCalledWith(catalogo[0], '09:00');
    });
});
