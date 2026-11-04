import { FormEvent, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { listarItinerariosDeDestino } from '../services/destinoService';
import { crearReserva } from '../services/reservaService';
import { obtenerMensajeError } from '../services/apiClient';
import { ItinerarioDisponible } from '../types/itinerario';
import { CrearReservaRequest } from '../types/reserva';
import { fechaMinimaReserva, formatearFecha, formatearRangoFechas } from '../utils/fechasItinerario';
import { formatearPrecio } from '../utils/formatearPrecio';

/** Reserva de un viaje: primero se elige uno de los itinerarios del destino y luego el día dentro de sus fechas. */
const FormularioReserva = () => {
    const { destinoId } = useParams<{ destinoId: string }>();
    const navigate = useNavigate();
    const [itinerarios, setItinerarios] = useState<ItinerarioDisponible[] | null>(null);
    const [errorCarga, setErrorCarga] = useState<string | null>(null);
    const [itinerarioId, setItinerarioId] = useState<number | null>(null);
    const [fechaReserva, setFechaReserva] = useState('');
    const [error, setError] = useState<string | null>(null);
    const [enviando, setEnviando] = useState(false);

    useEffect(() => {
        let activo = true;
        listarItinerariosDeDestino(Number(destinoId))
            .then((lista) => {
                if (!activo) return;
                setItinerarios(lista);
                if (lista.length === 1) setItinerarioId(lista[0].id);
            })
            .catch((err: unknown) => {
                if (activo) setErrorCarga(obtenerMensajeError(err, 'No pudimos cargar las fechas disponibles.'));
            });
        return () => {
            activo = false;
        };
    }, [destinoId]);

    const elegido = itinerarios?.find((i) => i.id === itinerarioId) ?? null;
    const minimo = elegido ? fechaMinimaReserva(elegido.fechaInicio) : undefined;
    const maximo = elegido?.fechaFin;

    const handleElegirItinerario = (itinerario: ItinerarioDisponible) => {
        setItinerarioId(itinerario.id);
        const nuevoMinimo = fechaMinimaReserva(itinerario.fechaInicio);
        if (fechaReserva && (fechaReserva < nuevoMinimo || fechaReserva > itinerario.fechaFin)) setFechaReserva('');
    };

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault();
        if (itinerarioId === null) return;
        setError(null);
        setEnviando(true);
        try {
            const nuevaReserva: CrearReservaRequest = { itinerarioId, fechaReserva };
            const reserva = await crearReserva(nuevaReserva);
            navigate('/confirmacion', { state: { reserva } }); // Redirección tras un 201 Created
        } catch (err) {
            setError(obtenerMensajeError(err, 'No se pudo crear la reserva. Intenta de nuevo.'));
        } finally {
            setEnviando(false);
        }
    };

    const sinItinerarios = itinerarios !== null && itinerarios.length === 0;
    const puedeEnviar = elegido !== null && fechaReserva !== '' && !enviando;

    return (
        <main className="reserva">
            <h1>Crear reserva</h1>

            {errorCarga && (
                <p role="alert" className="reserva__aviso reserva__aviso--error">
                    {errorCarga}
                </p>
            )}
            {itinerarios === null && !errorCarga && <p role="status">Cargando fechas disponibles…</p>}
            {sinItinerarios && (
                <p role="status" className="reserva__aviso">
                    Este destino no tiene fechas disponibles por ahora. Vuelve a consultar más adelante.
                </p>
            )}

            <form onSubmit={handleSubmit} className="reserva__formulario">
                {itinerarios !== null && itinerarios.length > 0 && (
                    <fieldset className="reserva__opciones">
                        <legend>Elige tu viaje</legend>
                        {itinerarios.map((itinerario) => (
                            <label
                                key={itinerario.id}
                                className={`reserva__opcion ${itinerario.id === itinerarioId ? 'reserva__opcion--elegida' : ''}`}
                            >
                                <input
                                    type="radio"
                                    name="itinerario"
                                    checked={itinerario.id === itinerarioId}
                                    onChange={() => handleElegirItinerario(itinerario)}
                                />
                                <span className="reserva__opcion-nombre">{itinerario.nombre}</span>
                                <span>{formatearRangoFechas(itinerario.fechaInicio, itinerario.fechaFin)}</span>
                                <span className="reserva__opcion-precio">{formatearPrecio(itinerario.precioBase)}</span>
                                <span>
                                    {itinerario.cuposDisponibles === 1
                                        ? 'Queda 1 cupo'
                                        : `Quedan ${itinerario.cuposDisponibles} cupos`}
                                </span>
                                {itinerario.id === itinerarioId && (
                                    <strong className="reserva__opcion-marca">✓ Elegido</strong>
                                )}
                            </label>
                        ))}
                    </fieldset>
                )}

                <label className="reserva__campo">
                    Día de tu reserva
                    <input
                        type="date"
                        value={fechaReserva}
                        min={minimo}
                        max={maximo}
                        disabled={!elegido}
                        onChange={(e) => setFechaReserva(e.target.value)}
                        required
                    />
                    <small>
                        {elegido
                            ? `Elige un día entre el ${formatearFecha(minimo!)} y el ${formatearFecha(maximo!)}.`
                            : 'Primero elige tu viaje.'}
                    </small>
                </label>

                {error && (
                    <p role="alert" className="reserva__aviso reserva__aviso--error">
                        {error}
                    </p>
                )}

                <button type="submit" className="boton boton--verde reserva__enviar" disabled={!puedeEnviar}>
                    {enviando ? 'Reservando…' : 'Confirmar reserva'}
                </button>
            </form>
        </main>
    );
};

export default FormularioReserva;
