import { Link, useLocation } from 'react-router-dom';
import EstadoReserva from '../components/EstadoReserva';
import { Reserva } from '../types/reserva';
import { formatearFecha } from '../utils/fechasItinerario';
import { formatearPrecio } from '../utils/formatearPrecio';

interface EstadoNavegacion {
    reserva?: Reserva;
}

/** Recibe la reserva recién creada por el estado de navegación; si se entra directo, muestra solo el aviso general. */
const ConfirmacionReserva = () => {
    const reserva = (useLocation().state as EstadoNavegacion | null)?.reserva;

    return (
        <main className="reserva reserva--confirmacion">
            <h1>¡Reserva creada!</h1>
            {reserva ? (
                <>
                    <dl className="reserva__resumen">
                        {reserva.destinoNombre && (
                            <div>
                                <dt>Destino</dt>
                                <dd>{reserva.destinoNombre}</dd>
                            </div>
                        )}
                        {reserva.itinerarioNombre && (
                            <div>
                                <dt>Viaje</dt>
                                <dd>{reserva.itinerarioNombre}</dd>
                            </div>
                        )}
                        <div>
                            <dt>Día</dt>
                            <dd>{formatearFecha(reserva.fechaReserva)}</dd>
                        </div>
                        <div>
                            <dt>Total</dt>
                            <dd>{formatearPrecio(reserva.montoTotal)}</dd>
                        </div>
                        <div>
                            <dt>Estado</dt>
                            <dd>
                                <EstadoReserva estado={reserva.estadoReserva} />
                            </dd>
                        </div>
                    </dl>
                    <p>Tu reserva quedó registrada. Te avisaremos cuando sea confirmada.</p>
                </>
            ) : (
                <p>Tu reserva quedó registrada. Puedes ver su estado en tus reservas.</p>
            )}
            <div className="reserva__acciones">
                <Link to="/perfil?seccion=reservas" className="boton boton--verde">
                    Mis reservas
                </Link>
                <Link to="/" className="boton boton--naranja">
                    Volver al inicio
                </Link>
            </div>
        </main>
    );
};

export default ConfirmacionReserva;
