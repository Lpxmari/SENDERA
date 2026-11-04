import { FormEvent, useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import AgregarActividad from '../components/itinerario/AgregarActividad';
import FilaActividad from '../components/itinerario/FilaActividad';
import IconoAdmin from '../components/admin/IconoAdmin';
import { fetchActividades } from '../services/actividadService';
import { obtenerMensajeError } from '../services/apiClient';
import { actualizarViaje, crearViaje, fetchViaje } from '../services/viajeService';
import { Actividad, ActividadEnViaje, GuardarViajeRequest } from '../types/viaje';
import { describirSolape, detectarSolapes, diasDelViaje, formatearDia, hoyIso, ordenarPorHora } from '../utils/itinerario';
import '../itinerario.css';

const MAX_ACTIVIDADES = 50;
const MAX_NOMBRE = 120;

let contadorClaves = 0;
const nuevaClave = (): string => `actividad-${++contadorClaves}`;

/** Crear (/itinerarios/nuevo) y editar (/itinerarios/:id/editar) un itinerario personal. */
const CrearItinerario = () => {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();
    const editando = id !== undefined;
    const viajeId = Number(id);

    const [nombre, setNombre] = useState('');
    const [fechaInicio, setFechaInicio] = useState('');
    const [fechaFin, setFechaFin] = useState('');
    const [actividades, setActividades] = useState<ActividadEnViaje[]>([]);
    const [catalogo, setCatalogo] = useState<Actividad[]>([]);
    const [inicioOriginal, setInicioOriginal] = useState<string | null>(null);

    const [cargando, setCargando] = useState(true);
    const [errorCarga, setErrorCarga] = useState<string | null>(null);
    const [errorNombre, setErrorNombre] = useState<string | null>(null);
    const [errorGuardado, setErrorGuardado] = useState<string | null>(null);
    const [guardando, setGuardando] = useState(false);

    useEffect(() => {
        let activo = true;
        const cargar = async () => {
            try {
                if (editando && !Number.isInteger(viajeId)) throw new Error('id inválido');
                const [actividadesCatalogo, viaje] = await Promise.all([
                    fetchActividades(),
                    editando ? fetchViaje(viajeId) : Promise.resolve(null),
                ]);
                if (!activo) return;
                setCatalogo(actividadesCatalogo);
                if (viaje) {
                    setNombre(viaje.nombre);
                    setFechaInicio(viaje.fechaInicio);
                    setFechaFin(viaje.fechaFin);
                    setInicioOriginal(viaje.fechaInicio);
                    setActividades(
                        viaje.actividades.map((a) => ({
                            clave: nuevaClave(),
                            actividadId: a.actividadId,
                            nombre: a.nombre,
                            ubicacion: a.ubicacion,
                            duracionMinutos: a.duracionMinutos,
                            dificultad: a.dificultad,
                            fecha: a.fecha,
                            horaInicio: a.horaInicio,
                        })),
                    );
                }
            } catch (error) {
                if (activo) setErrorCarga(obtenerMensajeError(error, 'No se pudo cargar el itinerario. Intenta de nuevo.'));
            } finally {
                if (activo) setCargando(false);
            }
        };
        void cargar();
        return () => {
            activo = false;
        };
    }, [editando, viajeId]);

    const hoy = hoyIso();
    const dias = useMemo(() => diasDelViaje(fechaInicio, fechaFin), [fechaInicio, fechaFin]);

    const errorFechas = useMemo((): string | null => {
        if (fechaInicio && fechaFin && fechaFin < fechaInicio) {
            return 'La fecha de finalización no puede ser anterior a la de inicio.';
        }
        // Un viaje ya iniciado puede editarse sin mover su fecha de inicio; cambiarla al pasado no.
        const inicioCambia = fechaInicio !== inicioOriginal;
        if (fechaInicio && inicioCambia && fechaInicio < hoy) {
            return 'No puedes usar fechas pasadas. Elige una fecha de inicio desde hoy.';
        }
        return null;
    }, [fechaInicio, fechaFin, inicioOriginal, hoy]);

    const solapes = useMemo(() => detectarSolapes(actividades), [actividades]);
    const clavesEnConflicto = useMemo(
        () => new Set(solapes.flatMap((s) => [s.primera.clave, s.segunda.clave])),
        [solapes],
    );
    const fechasValidas = Boolean(fechaInicio && fechaFin) && !errorFechas;
    const fueraDeRango = useMemo(
        () => (fechasValidas ? actividades.filter((a) => !dias.includes(a.fecha)) : []),
        [actividades, dias, fechasValidas],
    );

    const hayProblemas = solapes.length > 0 || fueraDeRango.length > 0 || errorFechas !== null;

    const agregar = (fecha: string, actividad: Actividad, horaInicio: string) =>
        setActividades((actuales) => [
            ...actuales,
            {
                clave: nuevaClave(),
                actividadId: actividad.id,
                nombre: actividad.nombre,
                ubicacion: actividad.ubicacion,
                duracionMinutos: actividad.duracionMinutos,
                dificultad: actividad.dificultad,
                fecha,
                horaInicio,
            },
        ]);

    const cambiarHora = (clave: string, horaInicio: string) =>
        setActividades((actuales) => actuales.map((a) => (a.clave === clave ? { ...a, horaInicio } : a)));

    const eliminar = (clave: string) => setActividades((actuales) => actuales.filter((a) => a.clave !== clave));

    /**
     * "Cambiar orden": la hora define el orden, así que la actividad intercambia su hora de inicio con la vecina
     * del mismo día (la anterior o la siguiente). Si eso provoca un solape, el aviso de la página lo muestra.
     */
    const mover = (clave: string, direccion: 'arriba' | 'abajo') =>
        setActividades((actuales) => {
            const actividad = actuales.find((a) => a.clave === clave);
            if (!actividad) return actuales;
            const delDia = ordenarPorHora(actuales.filter((a) => a.fecha === actividad.fecha));
            const posicion = delDia.findIndex((a) => a.clave === clave);
            const vecina = delDia[direccion === 'arriba' ? posicion - 1 : posicion + 1];
            if (!vecina) return actuales;
            return actuales.map((a) => {
                if (a.clave === actividad.clave) return { ...a, horaInicio: vecina.horaInicio };
                if (a.clave === vecina.clave) return { ...a, horaInicio: actividad.horaInicio };
                return a;
            });
        });

    const handleSubmit = async (evento: FormEvent) => {
        evento.preventDefault();
        setErrorGuardado(null);
        if (!nombre.trim()) {
            setErrorNombre('Escribe el nombre del viaje.');
            return;
        }
        setErrorNombre(null);
        if (!fechaInicio || !fechaFin) {
            setErrorGuardado('Indica las fechas de inicio y de finalización del viaje.');
            return;
        }
        if (hayProblemas) return;

        const cuerpo: GuardarViajeRequest = {
            nombre: nombre.trim(),
            fechaInicio,
            fechaFin,
            actividades: ordenarPorHora(actividades)
                .sort((a, b) => a.fecha.localeCompare(b.fecha))
                .map((a) => ({ actividadId: a.actividadId, fecha: a.fecha, horaInicio: a.horaInicio })),
        };
        setGuardando(true);
        try {
            if (editando) await actualizarViaje(viajeId, cuerpo);
            else await crearViaje(cuerpo);
            navigate('/itinerarios', { state: { guardado: cuerpo.nombre } });
        } catch (error) {
            setErrorGuardado(obtenerMensajeError(error, 'No se pudo guardar el itinerario. Intenta de nuevo.'));
            setGuardando(false);
        }
    };

    const titulo = editando ? 'Editar mi itinerario' : 'Crear mi itinerario';

    if (cargando) {
        return (
            <main className="itinerario">
                <p role="status">Cargando…</p>
            </main>
        );
    }

    if (errorCarga) {
        return (
            <main className="itinerario">
                <h1 className="itinerario__titulo">{titulo}</h1>
                <p role="alert" className="itinerario__error">
                    {errorCarga}
                </p>
            </main>
        );
    }

    const minInicio = inicioOriginal !== null && inicioOriginal < hoy ? inicioOriginal : hoy;

    return (
        <main className="itinerario">
            <h1 className="itinerario__titulo">{titulo}</h1>
            <p className="itinerario__subtitulo">Define las fechas y organiza las actividades de tu viaje.</p>

            <form onSubmit={handleSubmit} noValidate>
                <div className="itinerario__campo">
                    <label htmlFor="viaje-nombre">Nombre del viaje</label>
                    <input
                        id="viaje-nombre"
                        type="text"
                        value={nombre}
                        maxLength={MAX_NOMBRE}
                        placeholder="Ej. Viaje al Quindío"
                        aria-invalid={errorNombre !== null}
                        aria-describedby={errorNombre ? 'viaje-nombre-error' : undefined}
                        onChange={(evento) => setNombre(evento.target.value)}
                    />
                    {errorNombre && (
                        <p id="viaje-nombre-error" role="alert" className="itinerario__error">
                            {errorNombre}
                        </p>
                    )}
                </div>

                <div className="itinerario__fechas">
                    <div className="itinerario__campo">
                        <label htmlFor="viaje-inicio">Fecha de inicio</label>
                        <input
                            id="viaje-inicio"
                            type="date"
                            value={fechaInicio}
                            min={minInicio}
                            aria-invalid={errorFechas !== null}
                            aria-describedby={errorFechas ? 'viaje-fechas-error' : undefined}
                            onChange={(evento) => setFechaInicio(evento.target.value)}
                        />
                    </div>
                    <div className="itinerario__campo">
                        <label htmlFor="viaje-fin">Fecha de finalización</label>
                        <input
                            id="viaje-fin"
                            type="date"
                            value={fechaFin}
                            min={fechaInicio || minInicio}
                            aria-invalid={errorFechas !== null}
                            aria-describedby={errorFechas ? 'viaje-fechas-error' : undefined}
                            onChange={(evento) => setFechaFin(evento.target.value)}
                        />
                    </div>
                </div>
                {errorFechas && (
                    <p id="viaje-fechas-error" role="alert" className="itinerario__error">
                        {errorFechas}
                    </p>
                )}

                <h2 className="itinerario__seccion">Actividades de mi viaje</h2>

                {!fechasValidas && <p className="itinerario__ayuda">Elige las fechas del viaje para poder agregar actividades.</p>}

                {dias.map((dia, indice) => {
                    const delDia = ordenarPorHora(actividades.filter((a) => a.fecha === dia));
                    const nombreDia = `Día ${indice + 1} - ${formatearDia(dia)}`;
                    return (
                        <section key={dia} className="itinerario__dia" aria-label={nombreDia}>
                            <h3>{nombreDia}</h3>
                            {delDia.length === 0 ? (
                                <p className="itinerario__ayuda">Aún no hay actividades este día.</p>
                            ) : (
                                <ul className="itinerario__lista">
                                    {delDia.map((actividad, posicion) => (
                                        <FilaActividad
                                            key={actividad.clave}
                                            actividad={actividad}
                                            enConflicto={clavesEnConflicto.has(actividad.clave)}
                                            puedeSubir={posicion > 0}
                                            puedeBajar={posicion < delDia.length - 1}
                                            onCambiarHora={(hora) => cambiarHora(actividad.clave, hora)}
                                            onMover={(direccion) => mover(actividad.clave, direccion)}
                                            onEliminar={() => eliminar(actividad.clave)}
                                        />
                                    ))}
                                </ul>
                            )}
                            <AgregarActividad
                                catalogo={catalogo}
                                nombreDia={nombreDia}
                                deshabilitado={actividades.length >= MAX_ACTIVIDADES}
                                onAgregar={(actividad, hora) => agregar(dia, actividad, hora)}
                            />
                        </section>
                    );
                })}

                {fueraDeRango.length > 0 && (
                    <section className="itinerario__dia" aria-label="Actividades fuera de las fechas del viaje">
                        <h3>Fuera de las fechas del viaje</h3>
                        <ul className="itinerario__lista">
                            {fueraDeRango.map((actividad) => (
                                <FilaActividad
                                    key={actividad.clave}
                                    actividad={actividad}
                                    enConflicto={false}
                                    puedeSubir={false}
                                    puedeBajar={false}
                                    onCambiarHora={(hora) => cambiarHora(actividad.clave, hora)}
                                    onMover={() => undefined}
                                    onEliminar={() => eliminar(actividad.clave)}
                                />
                            ))}
                        </ul>
                    </section>
                )}

                {solapes.length > 0 && (
                    <div className="itinerario__aviso" role="alert">
                        <IconoAdmin nombre="alerta" />
                        <div>
                            <p>Hay un conflicto de horario entre estas actividades. Revisa los horarios antes de guardar.</p>
                            <ul>
                                {solapes.map((solape) => (
                                    <li key={`${solape.primera.clave}-${solape.segunda.clave}`}>{describirSolape(solape)}</li>
                                ))}
                            </ul>
                        </div>
                    </div>
                )}

                {fueraDeRango.length > 0 && (
                    <div className="itinerario__aviso" role="alert">
                        <IconoAdmin nombre="alerta" />
                        <p>
                            Tienes actividades fuera de las fechas del viaje. Elimínalas o cambia las fechas antes de guardar.
                        </p>
                    </div>
                )}

                {errorGuardado && (
                    <p role="alert" className="itinerario__error">
                        {errorGuardado}
                    </p>
                )}

                <div className="itinerario__guardar">
                    <button type="submit" className="boton boton--verde itinerario__boton-guardar" disabled={guardando || hayProblemas}>
                        {guardando ? 'Guardando…' : 'Guardar itinerario'}
                    </button>
                </div>
            </form>
        </main>
    );
};

export default CrearItinerario;
