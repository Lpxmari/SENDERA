import { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import IconoAdmin from '../components/admin/IconoAdmin';
import IconoUbicacion from '../components/IconoUbicacion';
import { obtenerMensajeError } from '../services/apiClient';
import { fetchViajes } from '../services/viajeService';
import { ViajeResumen } from '../types/viaje';
import { formatearHora, formatearRangoFechas } from '../utils/itinerario';
import '../itinerario.css';

const textoActividades = (total: number): string => (total === 1 ? '1 actividad' : `${total} actividades`);

/** Un viaje de un solo día y una sola actividad muestra también la hora; en los demás la hora sería ambigua. */
const textoFechas = (viaje: ViajeResumen): string => {
    const fechas = formatearRangoFechas(viaje.fechaInicio, viaje.fechaFin);
    const mostrarHora = viaje.totalActividades === 1 && viaje.fechaInicio === viaje.fechaFin && viaje.primeraHora;
    return mostrarHora ? `${fechas} · ${formatearHora(viaje.primeraHora as string)}` : fechas;
};

/** "Mis itinerarios y reservas" (/itinerarios): los itinerarios de la persona con sesión iniciada. */
const ListaItinerarios = () => {
    const { state } = useLocation();
    const navigate = useNavigate();
    // El aviso de "guardado"/"eliminado" llega en el estado de la navegación; se conserva en la página y se borra del
    // historial para que no reaparezca al recargar.
    const [{ guardado, eliminado }] = useState(() => (state as { guardado?: string; eliminado?: string } | null) ?? {});
    useEffect(() => {
        if (state) navigate('.', { replace: true, state: null });
    }, [state, navigate]);

    const [viajes, setViajes] = useState<ViajeResumen[]>([]);
    const [cargando, setCargando] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        let activo = true;
        const cargar = async () => {
            try {
                const lista = await fetchViajes();
                if (activo) setViajes(lista);
            } catch (excepcion) {
                if (activo) setError(obtenerMensajeError(excepcion, 'No se pudieron cargar tus itinerarios. Intenta de nuevo.'));
            } finally {
                if (activo) setCargando(false);
            }
        };
        void cargar();
        return () => {
            activo = false;
        };
    }, []);

    return (
        <main className="itinerario">
            <div className="lista-itinerarios__encabezado">
                <div>
                    <h1 className="itinerario__titulo lista-itinerarios__titulo">Mis itinerarios y reservas</h1>
                    <p className="itinerario__subtitulo">Consulta y organiza tus próximos viajes.</p>
                </div>
                <Link to="/itinerarios/nuevo" className="boton boton--naranja lista-itinerarios__crear">
                    + Crear nuevo itinerario
                </Link>
            </div>

            {guardado && (
                <p role="status" className="lista-itinerarios__guardado">
                    <IconoAdmin nombre="check" />
                    Guardaste el itinerario «{guardado}».
                </p>
            )}

            {eliminado && (
                <p role="status" className="lista-itinerarios__guardado">
                    <IconoAdmin nombre="check" />
                    Eliminaste el itinerario «{eliminado}».
                </p>
            )}

            {cargando && <p role="status">Cargando…</p>}

            {error && (
                <p role="alert" className="itinerario__error">
                    {error}
                </p>
            )}

            {!cargando && !error && viajes.length === 0 && (
                <p className="itinerario__ayuda">Aún no tienes itinerarios. Crea el primero con el botón naranja.</p>
            )}

            <ul className="lista-itinerarios">
                {viajes.map((viaje) => (
                    <li key={viaje.id} className="viaje-tarjeta">
                        <div className="viaje-tarjeta__datos">
                            <h2>{viaje.nombre}</h2>
                            {viaje.ubicacion && (
                                <p>
                                    <IconoUbicacion />
                                    {viaje.ubicacion}
                                </p>
                            )}
                            <p>
                                <IconoAdmin nombre="reservas" />
                                {textoFechas(viaje)}
                            </p>
                            <span className="viaje-tarjeta__actividades">{textoActividades(viaje.totalActividades)}</span>
                        </div>
                        <div className="viaje-tarjeta__acciones">
                            <Link to={`/itinerarios/${viaje.id}`} className="accion-itinerario accion-itinerario--solida">
                                Ver itinerario <span className="sr-only">de {viaje.nombre}</span>
                            </Link>
                            <Link to={`/itinerarios/${viaje.id}/editar`} className="accion-itinerario">
                                Editar <span className="sr-only">{viaje.nombre}</span>
                            </Link>
                        </div>
                    </li>
                ))}
            </ul>
        </main>
    );
};

export default ListaItinerarios;
