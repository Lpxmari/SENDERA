import { useId, useMemo, useState } from 'react';
import { Actividad } from '../../types/viaje';
import { etiquetaDificultad, formatearDuracion } from '../../utils/itinerario';

interface Props {
    catalogo: Actividad[];
    /** Nombre del día, solo para que los lectores de pantalla distingan cada botón ("Día 2 - 16 de octubre"). */
    nombreDia: string;
    deshabilitado: boolean;
    onAgregar: (actividad: Actividad, horaInicio: string) => void;
}

const HORA_POR_DEFECTO = '09:00';

/** "+ Agregar actividad": despliega un selector del catálogo y la hora de inicio. */
const AgregarActividad = ({ catalogo, nombreDia, deshabilitado, onAgregar }: Props) => {
    const [abierto, setAbierto] = useState(false);
    const [seleccion, setSeleccion] = useState('');
    const [hora, setHora] = useState(HORA_POR_DEFECTO);
    const idSelect = useId();
    const idHora = useId();

    const porDestino = useMemo(() => {
        const grupos = new Map<string, Actividad[]>();
        for (const actividad of catalogo) {
            grupos.set(actividad.destinoNombre, [...(grupos.get(actividad.destinoNombre) ?? []), actividad]);
        }
        return [...grupos.entries()];
    }, [catalogo]);

    const elegida = catalogo.find((a) => String(a.id) === seleccion);

    const abrir = () => {
        setSeleccion(catalogo.length > 0 ? String(catalogo[0].id) : '');
        setHora(HORA_POR_DEFECTO);
        setAbierto(true);
    };

    const handleAgregar = () => {
        if (!elegida || !hora) return;
        onAgregar(elegida, hora);
        setAbierto(false);
    };

    if (!abierto) {
        return (
            <button
                type="button"
                className="accion-itinerario agregar-actividad__abrir"
                disabled={deshabilitado || catalogo.length === 0}
                aria-label={`Agregar actividad al ${nombreDia}`}
                onClick={abrir}
            >
                + Agregar actividad
            </button>
        );
    }

    return (
        <div className="agregar-actividad" role="group" aria-label={`Agregar una actividad al ${nombreDia}`}>
            <div className="agregar-actividad__campo">
                <label htmlFor={idSelect}>Actividad</label>
                <select id={idSelect} value={seleccion} onChange={(evento) => setSeleccion(evento.target.value)}>
                    {porDestino.map(([destino, actividades]) => (
                        <optgroup key={destino} label={destino}>
                            {actividades.map((actividad) => (
                                <option key={actividad.id} value={actividad.id}>
                                    {actividad.nombre}
                                </option>
                            ))}
                        </optgroup>
                    ))}
                </select>
            </div>
            <div className="agregar-actividad__campo">
                <label htmlFor={idHora}>Hora de inicio</label>
                <input id={idHora} type="time" value={hora} onChange={(evento) => setHora(evento.target.value)} />
            </div>
            {elegida && (
                <p className="agregar-actividad__detalle">
                    {elegida.ubicacion} · {formatearDuracion(elegida.duracionMinutos)} · {etiquetaDificultad(elegida.dificultad)}
                </p>
            )}
            <div className="agregar-actividad__acciones">
                <button type="button" className="accion-itinerario accion-itinerario--solida" disabled={!elegida || !hora} onClick={handleAgregar}>
                    Agregar
                </button>
                <button type="button" className="accion-itinerario accion-itinerario--neutra" onClick={() => setAbierto(false)}>
                    Cancelar
                </button>
            </div>
        </div>
    );
};

export default AgregarActividad;
