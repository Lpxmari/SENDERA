import { useState } from 'react';
import { ActividadEnViaje } from '../../types/viaje';
import { etiquetaDificultad, formatearDuracion, formatearHora } from '../../utils/itinerario';
import IconoAdmin from '../admin/IconoAdmin';

interface Props {
    actividad: ActividadEnViaje;
    /** Se pisa con otra actividad del mismo día: se resalta y el aviso de la página lo explica. */
    enConflicto: boolean;
    puedeSubir: boolean;
    puedeBajar: boolean;
    onCambiarHora: (hora: string) => void;
    onMover: (direccion: 'arriba' | 'abajo') => void;
    onEliminar: () => void;
}

type Panel = 'orden' | 'horario' | null;

/** Una actividad del itinerario con sus tres acciones: cambiar orden, editar horario y eliminar. */
const FilaActividad = ({ actividad, enConflicto, puedeSubir, puedeBajar, onCambiarHora, onMover, onEliminar }: Props) => {
    const [panel, setPanel] = useState<Panel>(null);
    const alternar = (destino: Exclude<Panel, null>) => setPanel((actual) => (actual === destino ? null : destino));

    return (
        <li className={`actividad-fila ${enConflicto ? 'actividad-fila--conflicto' : ''}`.trim()}>
            <div className="actividad-fila__principal">
                <div className="actividad-fila__datos">
                    <h4>
                        {formatearHora(actividad.horaInicio)} - {actividad.nombre}
                    </h4>
                    <p className="actividad-fila__meta">
                        <span>
                            <IconoAdmin nombre="destinos" />
                            {actividad.ubicacion}
                        </span>
                        <span>
                            <IconoAdmin nombre="reloj" />
                            {formatearDuracion(actividad.duracionMinutos)}
                        </span>
                        <span className={`badge badge--dificultad-${actividad.dificultad.toLowerCase()}`}>
                            <span className="actividad-fila__punto" aria-hidden="true" />
                            {etiquetaDificultad(actividad.dificultad)}
                        </span>
                        {enConflicto && <span className="badge badge--conflicto">Se solapa con otra actividad</span>}
                    </p>
                </div>

                <div className="actividad-fila__acciones">
                    <button
                        type="button"
                        className="accion-itinerario accion-itinerario--neutra"
                        aria-expanded={panel === 'orden'}
                        aria-label={`Cambiar el orden de ${actividad.nombre}`}
                        onClick={() => alternar('orden')}
                    >
                        <IconoAdmin nombre="ordenar" />
                        Cambiar orden
                    </button>
                    <button
                        type="button"
                        className="accion-itinerario"
                        aria-expanded={panel === 'horario'}
                        aria-label={`Editar el horario de ${actividad.nombre}`}
                        onClick={() => alternar('horario')}
                    >
                        <IconoAdmin nombre="lapiz" />
                        Editar horario
                    </button>
                    <button
                        type="button"
                        className="accion-itinerario accion-itinerario--peligro"
                        aria-label={`Eliminar ${actividad.nombre}`}
                        onClick={onEliminar}
                    >
                        <IconoAdmin nombre="papelera" />
                        Eliminar
                    </button>
                </div>
            </div>

            {panel === 'orden' && (
                <div className="actividad-fila__panel" role="group" aria-label={`Orden de ${actividad.nombre}`}>
                    <p>La lista va por hora: al moverla, esta actividad intercambia su hora de inicio con la de al lado.</p>
                    <button type="button" className="accion-itinerario accion-itinerario--neutra" disabled={!puedeSubir} onClick={() => onMover('arriba')}>
                        Subir
                    </button>
                    <button type="button" className="accion-itinerario accion-itinerario--neutra" disabled={!puedeBajar} onClick={() => onMover('abajo')}>
                        Bajar
                    </button>
                    <button type="button" className="accion-itinerario" onClick={() => setPanel(null)}>
                        Listo
                    </button>
                </div>
            )}

            {panel === 'horario' && (
                <div className="actividad-fila__panel">
                    <label>
                        Hora de inicio de {actividad.nombre}
                        <input
                            type="time"
                            value={actividad.horaInicio}
                            onChange={(evento) => evento.target.value && onCambiarHora(evento.target.value)}
                        />
                    </label>
                    <button type="button" className="accion-itinerario" onClick={() => setPanel(null)}>
                        Listo
                    </button>
                </div>
            )}
        </li>
    );
};

export default FilaActividad;
