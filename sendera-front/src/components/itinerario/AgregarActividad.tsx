import { useId, useMemo, useState } from 'react';
import { Actividad, DuracionActividad } from '../../types/viaje';
import { etiquetaDificultad, formatearDuracion } from '../../utils/itinerario';

interface Props {
    catalogo: Actividad[];
    /** Nombre del día, solo para que los lectores de pantalla distingan cada botón ("Día 2 - 16 de octubre"). */
    nombreDia: string;
    deshabilitado: boolean;
    /** Rango de duración activo; `undefined` = todas. El filtro lo resuelve el servidor (GET /actividades?duracion=). */
    duracion?: DuracionActividad;
    onCambiarDuracion: (duracion: DuracionActividad | undefined) => void;
    /** Mientras el servidor responde a un cambio de filtro. */
    filtrando?: boolean;
    errorFiltro?: string | null;
    onAgregar: (actividad: Actividad, horaInicio: string) => void;
}

const HORA_POR_DEFECTO = '09:00';

const OPCIONES_DURACION: { valor: DuracionActividad | undefined; texto: string }[] = [
    { valor: undefined, texto: 'Todas' },
    { valor: 'HASTA_60_MIN', texto: 'Hasta 1 hora' },
    { valor: 'DE_61_A_180_MIN', texto: 'De 1 a 3 horas' },
    { valor: 'MAS_DE_180_MIN', texto: 'Más de 3 horas' },
];

/** "+ Agregar actividad": despliega el filtro de duración, un selector del catálogo y la hora de inicio. */
const AgregarActividad = ({
    catalogo,
    nombreDia,
    deshabilitado,
    duracion,
    onCambiarDuracion,
    filtrando = false,
    errorFiltro = null,
    onAgregar,
}: Props) => {
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

    // Si el catálogo cambia (otro filtro), la selección anterior puede haber desaparecido: se usa la primera.
    const elegida = catalogo.find((a) => String(a.id) === seleccion) ?? catalogo[0];
    const valorSeleccionado = elegida ? String(elegida.id) : '';
    const sinResultados = catalogo.length === 0 && !filtrando;

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
                // Con un filtro activo y sin resultados se deja abrir para poder cambiarlo.
                disabled={deshabilitado || (catalogo.length === 0 && duracion === undefined)}
                aria-label={`Agregar actividad al ${nombreDia}`}
                onClick={abrir}
            >
                + Agregar actividad
            </button>
        );
    }

    return (
        <div className="agregar-actividad" role="group" aria-label={`Agregar una actividad al ${nombreDia}`}>
            <fieldset className="filtro-duracion">
                <legend>Duración de la actividad</legend>
                <div className="filtro-duracion__botones">
                    {OPCIONES_DURACION.map((opcion) => (
                        <button
                            key={opcion.texto}
                            type="button"
                            className="filtro-duracion__boton"
                            aria-pressed={duracion === opcion.valor}
                            onClick={() => onCambiarDuracion(opcion.valor)}
                        >
                            {opcion.texto}
                        </button>
                    ))}
                </div>
            </fieldset>
            {filtrando && (
                <p role="status" className="agregar-actividad__vacio">
                    Buscando actividades…
                </p>
            )}
            {errorFiltro && (
                <p role="alert" className="itinerario__error">
                    {errorFiltro}
                </p>
            )}
            {sinResultados && !errorFiltro && (
                <p role="status" className="agregar-actividad__vacio">
                    No hay actividades con esa duración. Prueba con otra opción.
                </p>
            )}
            <div className="agregar-actividad__campo">
                <label htmlFor={idSelect}>Actividad</label>
                <select id={idSelect} value={valorSeleccionado} onChange={(evento) => setSeleccion(evento.target.value)}>
                    {porDestino.map(([destino, actividades]) => (
                        <optgroup key={destino} label={destino}>
                            {actividades.map((actividad) => (
                                <option key={actividad.id} value={actividad.id}>
                                    {actividad.nombre} ({formatearDuracion(actividad.duracionMinutos)})
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
