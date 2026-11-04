import { EstadoReserva as Estado } from '../types/reserva';
import { ETIQUETAS_ESTADO } from '../utils/estadoReserva';

/** Estado de una reserva con símbolo y texto, para que no dependa solo del color. */
const EstadoReserva = ({ estado }: { estado: Estado }) => {
    const { texto, simbolo } = ETIQUETAS_ESTADO[estado];
    return (
        <span className={`perfil-estado perfil-estado--${estado.toLowerCase()}`}>
            <span aria-hidden="true">{simbolo} </span>
            {texto}
        </span>
    );
};

export default EstadoReserva;
