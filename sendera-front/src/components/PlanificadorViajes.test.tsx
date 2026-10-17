import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import PlanificadorViajes from './PlanificadorViajes';
import { sugerenciasViaje } from '../utils/sugerenciasViaje';

const renderizar = () =>
    render(
        <MemoryRouter>
            <PlanificadorViajes />
        </MemoryRouter>,
    );

describe('PlanificadorViajes', () => {
    it('cada sugerencia lleva al detalle de su alojamiento', () => {
        renderizar();

        const enlaces = screen.getAllByRole('link', { name: /Ver el detalle de/ });
        expect(enlaces).toHaveLength(sugerenciasViaje.length);
        sugerenciasViaje.forEach((s, i) => expect(enlaces[i]).toHaveAttribute('href', `/destinos/${s.destinoId}`));
    });

    it('al filtrar por estilo solo quedan las sugerencias de ese estilo, igualmente enlazadas', () => {
        renderizar();

        fireEvent.click(screen.getByRole('button', { name: 'Relajación Costera' }));

        const enlaces = screen.getAllByRole('link', { name: /Ver el detalle de/ });
        expect(enlaces).toHaveLength(1);
        expect(enlaces[0]).toHaveAttribute('href', '/destinos/3');
    });
});
