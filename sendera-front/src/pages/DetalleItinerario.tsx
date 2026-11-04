import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import DialogoConfirmacion from '../components/admin/DialogoConfirmacion';
import IconoAdmin from '../components/admin/IconoAdmin';
import IconoUbicacion from '../components/IconoUbicacion';
import { obtenerMensajeError } from '../services/apiClient';
import { eliminarViaje, fetchViaje } from '../services/viajeService';
import { Viaje, ViajeActividad } from '../types/viaje';
import { diasDelViaje, formatearDiaConSemana, formatearHora, formatearRangoFechas } from '../utils/itinerario';
import '../itinerario.css';

/** Ver un itinerario (/itinerarios/:id): solo lectura, con accesos para editarlo o eliminarlo. */
const DetalleItinerario = () => {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();
    const viajeId = Number(id);

    const [viaje, setViaje] = useState<Viaje | null>(null);
    const [cargando, setCargando] = useState(true);
    const [errorCarga, setErrorCarga] = useState<string | null>(null);
    const [confirmando, setConfirmando] = useState(false);
    const [eliminando, setEliminando] = useState(false);
    const [errorEliminar, setErrorEliminar] = useState<string | null>(null);

    useEffect(() => {
        let activo = true;
        const cargar = async () => {
            try {
                if (!Number.isInteger(viajeId)) throw new Error('id inválido');
                const datos = await fetchViaje(viajeId);
                if (activo) setViaje(datos);
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
    }, [viajeId]);

    const actividadesPorDia = useMemo(() => {
        const porDia = new Map<string, ViajeActividad[]>();
        for (const actividad of viaje?.actividades ?? []) {
            porDia.set(actividad.fecha, [...(porDia.get(actividad.fecha) ?? []), actividad]);
        }
        return porDia;
    }, [viaje]);

    const handleEliminar = async () => {
        setEliminando(true);
        setErrorEliminar(null);
        try {
            await eliminarViaje(viajeId);
            navigate('/itinerarios', { state: { eliminado: viaje?.nombre } });
        } catch (error) {
            setErrorEliminar(obtenerMensajeError(error, 'No se pudo eliminar el itinerario. Intenta de nuevo.'));
            setConfirmando(false);
            setEliminando(false);
        }
    };

    if (cargando) {
        return (
            <main className="itinerario detalle-itinerario">
                <p role="status">Cargando…</p>
            </main>
        );
    }

    if (errorCarga || !viaje) {
        return (
            <main className="itinerario detalle-itinerario">
                <p role="alert" className="itinerario__error">
                    {errorCarga}
                </p>
                <Link to="/itinerarios" className="detalle-itinerario__volver">
                    <IconoAdmin nombre="izquierda" />
                    Volver a Mis itinerarios
                </Link>
            </main>
        );
    }

    // Misma regla que la lista: el lugar del viaje es el de su primera actividad
    const ubicacion = viaje.actividades[0]?.ubicacion;
    const dias = diasDelViaje(viaje.fechaInicio, viaje.fechaFin);

    return (
        <main className="itinerario detalle-itinerario">
            <h1 className="itinerario__titulo lista-itinerarios__titulo">{viaje.nombre}</h1>
            <Link to="/itinerarios" className="detalle-itinerario__volver">
                <IconoAdmin nombre="izquierda" />
                Volver a Mis itinerarios
            </Link>

            <section className="detalle-itinerario__resumen" aria-label="Datos del viaje">
                {ubicacion && (
                    <p>
                        <IconoUbicacion />
                        {ubicacion}
                    </p>
                )}
                <p>
                    <IconoAdmin nombre="reservas" />
                    {formatearRangoFechas(viaje.fechaInicio, viaje.fechaFin)}
                </p>
            </section>

            <h2 className="itinerario__seccion">Actividades de mi viaje</h2>
            {viaje.actividades.length === 0 && <p className="itinerario__ayuda">Este itinerario aún no tiene actividades.</p>}

            {dias.map((dia, indice) => {
                const delDia = actividadesPorDia.get(dia);
                if (!delDia) return null;
                const nombreDia = `Día ${indice + 1} - ${formatearDiaConSemana(dia)}`;
                return (
                    <section key={dia} className="itinerario__dia" aria-label={nombreDia}>
                        <h3>{nombreDia}</h3>
                        <ul className="itinerario__lista">
                            {delDia.map((actividad) => (
                                <li key={actividad.id} className="detalle-actividad">
                                    <span className="detalle-actividad__icono">
                                        <IconoAdmin nombre="ruta" />
                                    </span>
                                    <div>
                                        <p className="detalle-actividad__hora">{formatearHora(actividad.horaInicio)}</p>
                                        <h4>{actividad.nombre}</h4>
                                        <p className="detalle-actividad__lugar">
                                            <IconoUbicacion />
                                            {actividad.ubicacion}
                                        </p>
                                    </div>
                                </li>
                            ))}
                        </ul>
                    </section>
                );
            })}

            {errorEliminar && (
                <p role="alert" className="itinerario__error">
                    {errorEliminar}
                </p>
            )}

            <div className="detalle-itinerario__acciones">
                <Link to={`/itinerarios/${viaje.id}/editar`} className="accion-itinerario accion-itinerario--solida">
                    <IconoAdmin nombre="lapiz" />
                    Editar itinerario
                </Link>
                <button type="button" className="accion-itinerario accion-itinerario--peligro" onClick={() => setConfirmando(true)}>
                    <IconoAdmin nombre="papelera" />
                    Eliminar itinerario
                </button>
                <Link to="/itinerarios" className="detalle-itinerario__volver detalle-itinerario__volver--centrado">
                    <IconoAdmin nombre="izquierda" />
                    Volver a mis itinerarios
                </Link>
            </div>

            {confirmando && (
                <DialogoConfirmacion
                    peligro
                    titulo="Eliminar itinerario"
                    mensaje={`¿Seguro que quieres eliminar «${viaje.nombre}»? Se borrarán también sus actividades. Esta acción no se puede deshacer.`}
                    textoConfirmar="Sí, eliminar"
                    procesando={eliminando}
                    onConfirmar={() => void handleEliminar()}
                    onCancelar={() => setConfirmando(false)}
                />
            )}
        </main>
    );
};

export default DetalleItinerario;
