import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { AxiosError } from 'axios';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fetchActividades } from '../services/actividadService';
import { actualizarViaje, crearViaje, fetchViaje } from '../services/viajeService';
import { Actividad, Viaje } from '../types/viaje';
import CrearItinerario from './CrearItinerario';

vi.mock('../services/actividadService');
vi.mock('../services/viajeService');

const INICIO = '2099-10-15';
const FIN = '2099-10-16';

const catalogo: Actividad[] = [
    {
        id: 1,
        destinoId: 10,
        destinoNombre: 'Filandia',
        ubicacion: 'Filandia, Quindío',
        nombre: 'Recorrido por Filandia',
        descripcion: 'Caminata',
        duracionMinutos: 180,
        dificultad: 'BAJA',
        precioPorPersona: 80000,
        accesibilidad: true,
    },
    {
        id: 2,
        destinoId: 10,
        destinoNombre: 'Filandia',
        ubicacion: 'Quindío',
        nombre: 'Visita a finca cafetera',
        descripcion: 'Café',
        duracionMinutos: 120,
        dificultad: 'MODERADA',
        precioPorPersona: 55000,
        accesibilidad: false,
    },
];

const viajeGuardado: Viaje = {
    id: 7,
    nombre: 'Viaje al Quindío',
    fechaInicio: INICIO,
    fechaFin: FIN,
    actividades: [
        { id: 1, actividadId: 1, destinoId: 10, nombre: 'Recorrido por Filandia', ubicacion: 'Filandia, Quindío', fecha: INICIO, horaInicio: '09:00', duracionMinutos: 180, dificultad: 'BAJA', orden: 1 },
        { id: 2, actividadId: 2, destinoId: 10, nombre: 'Visita a finca cafetera', ubicacion: 'Quindío', fecha: INICIO, horaInicio: '14:00', duracionMinutos: 120, dificultad: 'MODERADA', orden: 2 },
        { id: 3, actividadId: 1, destinoId: 10, nombre: 'Recorrido por Filandia', ubicacion: 'Filandia, Quindío', fecha: FIN, horaInicio: '10:00', duracionMinutos: 180, dificultad: 'BAJA', orden: 1 },
    ],
};

const renderizar = (ruta = '/itinerarios/nuevo') =>
    render(
        <MemoryRouter initialEntries={[ruta]}>
            <Routes>
                <Route path="/itinerarios/nuevo" element={<CrearItinerario />} />
                <Route path="/itinerarios/:id/editar" element={<CrearItinerario />} />
                <Route path="/itinerarios" element={<p>Lista de itinerarios</p>} />
            </Routes>
        </MemoryRouter>,
    );

const escribir = (etiqueta: string, valor: string) => fireEvent.change(screen.getByLabelText(etiqueta), { target: { value: valor } });

const esperarFormulario = async () => screen.findByLabelText('Nombre del viaje');

const rellenarFechas = (inicio = INICIO, fin = FIN) => {
    escribir('Fecha de inicio', inicio);
    escribir('Fecha de finalización', fin);
};

/** Agrega una actividad del catálogo al día indicado ("Día 1", "Día 2"…). */
const agregarActividad = (dia: string, idActividad: string, hora: string) => {
    fireEvent.click(screen.getByRole('button', { name: new RegExp(`Agregar actividad al ${dia}`) }));
    fireEvent.change(screen.getByLabelText('Actividad'), { target: { value: idActividad } });
    fireEvent.change(screen.getByLabelText('Hora de inicio'), { target: { value: hora } });
    fireEvent.click(screen.getByRole('button', { name: 'Agregar' }));
};

const botonGuardar = () => screen.getByRole('button', { name: /Guardar itinerario|Guardando/ });

const titulosDeActividades = () => screen.getAllByRole('heading', { level: 4 }).map((h) => h.textContent);

describe('CrearItinerario', () => {
    beforeEach(() => {
        vi.mocked(fetchActividades).mockResolvedValue(catalogo);
        vi.mocked(fetchViaje).mockResolvedValue(viajeGuardado);
        vi.mocked(crearViaje).mockResolvedValue(viajeGuardado);
        vi.mocked(actualizarViaje).mockResolvedValue(viajeGuardado);
    });

    it('muestra el formulario y pide las fechas antes de dejar agregar actividades', async () => {
        renderizar();
        await esperarFormulario();

        expect(screen.getByRole('heading', { level: 1, name: 'Crear mi itinerario' })).toBeInTheDocument();
        expect(screen.getByText('Elige las fechas del viaje para poder agregar actividades.')).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /Agregar actividad/ })).not.toBeInTheDocument();
    });

    it('arma un bloque por cada día del viaje', async () => {
        renderizar();
        await esperarFormulario();
        rellenarFechas(INICIO, '2099-10-17');

        expect(screen.getByRole('heading', { level: 3, name: 'Día 1 - 15 de octubre de 2099' })).toBeInTheDocument();
        expect(screen.getByRole('heading', { level: 3, name: 'Día 2 - 16 de octubre de 2099' })).toBeInTheDocument();
        expect(screen.getByRole('heading', { level: 3, name: 'Día 3 - 17 de octubre de 2099' })).toBeInTheDocument();
    });

    it('agrega una actividad del catálogo con su hora, ubicación, duración y dificultad', async () => {
        renderizar();
        await esperarFormulario();
        rellenarFechas();

        agregarActividad('Día 1', '1', '09:00');

        const titulo = screen.getByRole('heading', { level: 4, name: '9:00 a. m. - Recorrido por Filandia' });
        const fila = titulo.closest('li') as HTMLElement;
        expect(within(fila).getByText('Filandia, Quindío')).toBeInTheDocument();
        expect(within(fila).getByText('3 horas')).toBeInTheDocument();
        expect(within(fila).getByText('Dificultad baja')).toBeInTheDocument();
    });

    it('elimina una actividad', async () => {
        renderizar();
        await esperarFormulario();
        rellenarFechas();
        agregarActividad('Día 1', '1', '09:00');

        fireEvent.click(screen.getByRole('button', { name: 'Eliminar Recorrido por Filandia' }));

        expect(screen.queryByRole('heading', { level: 4 })).not.toBeInTheDocument();
        expect(screen.getAllByText('Aún no hay actividades este día.')).toHaveLength(2);
    });

    it('avisa del solape, resalta las actividades y bloquea el guardado hasta corregir el horario', async () => {
        renderizar();
        await esperarFormulario();
        escribir('Nombre del viaje', 'Viaje al Quindío');
        rellenarFechas();
        agregarActividad('Día 1', '1', '09:00'); // 09:00-12:00
        agregarActividad('Día 1', '2', '11:00'); // 11:00-13:00

        const aviso = screen.getByText('Hay un conflicto de horario entre estas actividades. Revisa los horarios antes de guardar.');
        expect(aviso).toBeInTheDocument();
        expect(screen.getByText(/«Recorrido por Filandia» \(9:00 a. m. – 12:00 p. m.\) y «Visita a finca cafetera» \(11:00 a. m. – 1:00 p. m.\) se solapan/)).toBeInTheDocument();
        expect(screen.getAllByText('Se solapa con otra actividad')).toHaveLength(2);
        expect(botonGuardar()).toBeDisabled();

        fireEvent.click(screen.getByRole('button', { name: 'Editar el horario de Visita a finca cafetera' }));
        fireEvent.change(screen.getByLabelText('Hora de inicio de Visita a finca cafetera'), { target: { value: '12:00' } });

        expect(screen.queryByText(/Hay un conflicto de horario/)).not.toBeInTheDocument();
        expect(botonGuardar()).toBeEnabled();
    });

    it('terminar justo cuando empieza la siguiente no se avisa como solape', async () => {
        renderizar();
        await esperarFormulario();
        rellenarFechas();
        agregarActividad('Día 1', '1', '09:00'); // hasta las 12:00
        agregarActividad('Día 1', '2', '12:00');

        expect(screen.queryByText(/Hay un conflicto de horario/)).not.toBeInTheDocument();
    });

    it('"Cambiar orden" intercambia la hora con la actividad de al lado y la lista sigue ordenada por hora', async () => {
        renderizar();
        await esperarFormulario();
        rellenarFechas();
        agregarActividad('Día 1', '1', '09:00');
        agregarActividad('Día 1', '2', '14:00');
        expect(titulosDeActividades()).toEqual(['9:00 a. m. - Recorrido por Filandia', '2:00 p. m. - Visita a finca cafetera']);

        fireEvent.click(screen.getByRole('button', { name: 'Cambiar el orden de Visita a finca cafetera' }));
        const panel = screen.getByRole('group', { name: 'Orden de Visita a finca cafetera' });
        expect(within(panel).getByRole('button', { name: 'Bajar' })).toBeDisabled();
        fireEvent.click(within(panel).getByRole('button', { name: 'Subir' }));

        // La finca toma las 9:00 y el recorrido pasa a las 14:00: la lista nunca queda fuera de orden cronológico
        expect(titulosDeActividades()).toEqual(['9:00 a. m. - Visita a finca cafetera', '2:00 p. m. - Recorrido por Filandia']);
        expect(within(screen.getByRole('group', { name: 'Orden de Visita a finca cafetera' })).getByRole('button', { name: 'Subir' })).toBeDisabled();
    });

    it('si al intercambiar las horas las duraciones hacen que se pisen, avisa del solape y bloquea el guardado', async () => {
        renderizar();
        await esperarFormulario();
        escribir('Nombre del viaje', 'Viaje');
        rellenarFechas();
        agregarActividad('Día 1', '2', '09:00'); // finca, 2 h: 09:00-11:00
        agregarActividad('Día 1', '1', '11:00'); // recorrido, 3 h: 11:00-14:00
        expect(screen.queryByText(/Hay un conflicto de horario/)).not.toBeInTheDocument();

        // El recorrido (3 h) pasa a las 09:00-12:00 y la finca a las 11:00-13:00: se pisan
        fireEvent.click(screen.getByRole('button', { name: 'Cambiar el orden de Recorrido por Filandia' }));
        fireEvent.click(screen.getByRole('button', { name: 'Subir' }));

        expect(titulosDeActividades()).toEqual(['9:00 a. m. - Recorrido por Filandia', '11:00 a. m. - Visita a finca cafetera']);
        expect(screen.getByText(/Hay un conflicto de horario/)).toBeInTheDocument();
        expect(botonGuardar()).toBeDisabled();
    });

    it('una actividad agregada con una hora más temprana se coloca antes y se guarda en orden cronológico', async () => {
        renderizar();
        await esperarFormulario();
        escribir('Nombre del viaje', 'Viaje');
        rellenarFechas();
        agregarActividad('Día 1', '2', '14:00');
        agregarActividad('Día 1', '1', '09:00');

        expect(titulosDeActividades()).toEqual(['9:00 a. m. - Recorrido por Filandia', '2:00 p. m. - Visita a finca cafetera']);

        fireEvent.click(botonGuardar());
        await waitFor(() => expect(crearViaje).toHaveBeenCalled());
        expect(vi.mocked(crearViaje).mock.calls[0][0].actividades.map((a) => a.horaInicio)).toEqual(['09:00', '14:00']);
    });

    it('rechaza una fecha de inicio pasada y bloquea el guardado', async () => {
        renderizar();
        await esperarFormulario();

        rellenarFechas('2020-01-01', '2020-01-03');

        expect(screen.getByText('No puedes usar fechas pasadas. Elige una fecha de inicio desde hoy.')).toBeInTheDocument();
        expect(botonGuardar()).toBeDisabled();
    });

    it('rechaza una fecha de fin anterior a la de inicio', async () => {
        renderizar();
        await esperarFormulario();

        rellenarFechas('2099-10-16', '2099-10-15');

        expect(screen.getByText('La fecha de finalización no puede ser anterior a la de inicio.')).toBeInTheDocument();
        expect(botonGuardar()).toBeDisabled();
    });

    it('exige un nombre y no llama al servidor sin él', async () => {
        renderizar();
        await esperarFormulario();
        rellenarFechas();

        fireEvent.click(botonGuardar());

        expect(await screen.findByText('Escribe el nombre del viaje.')).toBeInTheDocument();
        expect(crearViaje).not.toHaveBeenCalled();
    });

    it('guarda el itinerario con las actividades en el orden de la pantalla y vuelve a la lista', async () => {
        renderizar();
        await esperarFormulario();
        escribir('Nombre del viaje', '  Viaje al Quindío ');
        rellenarFechas();
        agregarActividad('Día 1', '1', '09:00');
        agregarActividad('Día 1', '2', '14:00');
        agregarActividad('Día 2', '1', '10:00');

        fireEvent.click(botonGuardar());

        await waitFor(() => expect(screen.getByText('Lista de itinerarios')).toBeInTheDocument());
        expect(crearViaje).toHaveBeenCalledWith({
            nombre: 'Viaje al Quindío',
            fechaInicio: INICIO,
            fechaFin: FIN,
            actividades: [
                { actividadId: 1, fecha: INICIO, horaInicio: '09:00' },
                { actividadId: 2, fecha: INICIO, horaInicio: '14:00' },
                { actividadId: 1, fecha: FIN, horaInicio: '10:00' },
            ],
        });
    });

    it('muestra el mensaje del servidor si rechaza el guardado y deja reintentar', async () => {
        vi.mocked(crearViaje).mockRejectedValue(
            Object.assign(new AxiosError('fallo'), { response: { status: 409, data: { message: 'Las actividades A y B se solapan.' } } }),
        );
        renderizar();
        await esperarFormulario();
        escribir('Nombre del viaje', 'Viaje');
        rellenarFechas();

        fireEvent.click(botonGuardar());

        expect(await screen.findByText('Las actividades A y B se solapan.')).toBeInTheDocument();
        expect(screen.queryByText('Lista de itinerarios')).not.toBeInTheDocument();
        expect(botonGuardar()).toBeEnabled();
    });

    it('muestra un error claro si no se puede cargar el catálogo', async () => {
        vi.mocked(fetchActividades).mockRejectedValue(new Error('sin red'));
        renderizar();

        expect(await screen.findByText('No se pudo cargar el itinerario. Intenta de nuevo.')).toBeInTheDocument();
    });

    describe('al editar', () => {
        it('carga el itinerario, ordena las actividades por día y guarda con PUT', async () => {
            renderizar('/itinerarios/7/editar');
            await esperarFormulario();

            expect(screen.getByRole('heading', { level: 1, name: 'Editar mi itinerario' })).toBeInTheDocument();
            expect(screen.getByLabelText('Nombre del viaje')).toHaveValue('Viaje al Quindío');
            expect(screen.getByLabelText('Fecha de inicio')).toHaveValue(INICIO);
            expect(titulosDeActividades()).toEqual([
                '9:00 a. m. - Recorrido por Filandia',
                '2:00 p. m. - Visita a finca cafetera',
                '10:00 a. m. - Recorrido por Filandia',
            ]);

            escribir('Nombre del viaje', 'Viaje editado');
            fireEvent.click(botonGuardar());

            await waitFor(() => expect(actualizarViaje).toHaveBeenCalled());
            expect(fetchViaje).toHaveBeenCalledWith(7);
            expect(crearViaje).not.toHaveBeenCalled();
            expect(vi.mocked(actualizarViaje).mock.calls[0][0]).toBe(7);
            expect(vi.mocked(actualizarViaje).mock.calls[0][1].nombre).toBe('Viaje editado');
            expect(vi.mocked(actualizarViaje).mock.calls[0][1].actividades).toHaveLength(3);
        });

        it('permite editar un viaje ya iniciado sin cambiar su fecha de inicio', async () => {
            vi.mocked(fetchViaje).mockResolvedValue({ ...viajeGuardado, fechaInicio: '2020-01-01', fechaFin: '2020-01-02', actividades: [] });
            renderizar('/itinerarios/7/editar');
            await esperarFormulario();

            expect(screen.queryByText(/No puedes usar fechas pasadas/)).not.toBeInTheDocument();
            expect(botonGuardar()).toBeEnabled();

            escribir('Fecha de inicio', '2019-12-30');
            expect(screen.getByText(/No puedes usar fechas pasadas/)).toBeInTheDocument();
        });

        it('si se acortan las fechas, avisa de las actividades que quedan fuera y bloquea el guardado', async () => {
            renderizar('/itinerarios/7/editar');
            await esperarFormulario();

            escribir('Fecha de finalización', INICIO);

            expect(screen.getByText(/Tienes actividades fuera de las fechas del viaje/)).toBeInTheDocument();
            expect(screen.getByRole('heading', { level: 3, name: 'Fuera de las fechas del viaje' })).toBeInTheDocument();
            expect(botonGuardar()).toBeDisabled();

            fireEvent.click(screen.getAllByRole('button', { name: 'Eliminar Recorrido por Filandia' })[1]);

            expect(screen.queryByText(/Tienes actividades fuera de las fechas del viaje/)).not.toBeInTheDocument();
            expect(botonGuardar()).toBeEnabled();
        });

        it('muestra el mensaje del servidor si el itinerario no existe o es de otra persona', async () => {
            vi.mocked(fetchViaje).mockRejectedValue(
                Object.assign(new AxiosError('no encontrado'), { response: { status: 404, data: { message: 'Itinerario no encontrado con id: 99' } } }),
            );
            renderizar('/itinerarios/99/editar');

            expect(await screen.findByText('Itinerario no encontrado con id: 99')).toBeInTheDocument();
        });

        it('un id que no es un número no llama al servidor', async () => {
            renderizar('/itinerarios/abc/editar');

            expect(await screen.findByRole('alert')).toBeInTheDocument();
            expect(fetchViaje).not.toHaveBeenCalled();
        });
    });
});
