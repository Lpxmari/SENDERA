import { Link, useLocation } from 'react-router-dom';
import BotonPagar from '../components/BotonPagar';
import ResumenReserva from '../components/ResumenReserva';
import { Reserva } from '../types/reserva';

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
                    <ResumenReserva reserva={reserva} />
                    {reserva.estadoReserva === 'PENDIENTE' ? (
                        <>
                            <p>Tu reserva quedó registrada. Págala con Mercado Pago para confirmarla.</p>
                            <BotonPagar reservaId={reserva.id} />
                        </>
                    ) : (
                        <p>Tu reserva quedó registrada. Te avisaremos cuando sea confirmada.</p>
                    )}
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
