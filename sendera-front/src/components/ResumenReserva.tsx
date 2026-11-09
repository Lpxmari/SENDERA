import { Reserva } from '../types/reserva';
import { formatearFecha } from '../utils/fechasItinerario';
import { formatearPrecio } from '../utils/formatearPrecio';
import EstadoReserva from './EstadoReserva';

/** Datos principales de una reserva (confirmación y resultado del pago). */
const ResumenReserva = ({ reserva }: { reserva: Reserva }) => (
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
);

export default ResumenReserva;
