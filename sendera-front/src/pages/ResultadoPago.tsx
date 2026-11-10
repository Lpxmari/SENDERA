import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import BotonPagar from '../components/BotonPagar';
import ResumenReserva from '../components/ResumenReserva';
import { obtenerMensajeError } from '../services/apiClient';
import { verificarPago } from '../services/reservaService';
import { Reserva } from '../types/reserva';

interface Mensaje {
    titulo: string;
    texto: string;
    /** Ofrecer pagar (o reintentar) con Mercado Pago. */
    pagar?: string;
    verificarDeNuevo?: boolean;
}

/** Qué decir según el estado que el backend consultó en Mercado Pago (no según la URL de retorno). */
const mensajePara = (reserva: Reserva): Mensaje => {
    if (reserva.estadoPago === 'APROBADO' || reserva.estadoReserva === 'CONFIRMADA')
        return { titulo: '¡Pago aprobado!', texto: 'Tu reserva quedó confirmada. ¡Buen viaje!' };
    if (reserva.estadoReserva === 'CANCELADA')
        return { titulo: 'Reserva cancelada', texto: 'Esta reserva fue cancelada y ya no se puede pagar.' };
    switch (reserva.estadoPago) {
        case 'RECHAZADO':
            return {
                titulo: 'El pago no se completó',
                texto: 'Mercado Pago no aprobó el pago y no se hizo ningún cobro. Puedes intentarlo de nuevo.',
                pagar: 'Intentar pagar de nuevo',
            };
        case 'REEMBOLSADO':
            return { titulo: 'Pago reembolsado', texto: 'El pago de esta reserva fue devuelto.' };
        case 'PENDIENTE':
            return {
                titulo: 'Tu pago está en proceso',
                texto: 'Mercado Pago aún no confirma el pago. Si ya pagaste, espera unos minutos y vuelve a verificar.',
                pagar: 'Pagar con Mercado Pago',
                verificarDeNuevo: true,
            };
        default:
            return {
                titulo: 'Aún no registramos tu pago',
                texto: 'Paga tu reserva con Mercado Pago para confirmarla.',
                pagar: 'Pagar con Mercado Pago',
            };
    }
};

/** Página de retorno de Mercado Pago (back_urls): /pago/resultado?reserva={id}&... */
const ResultadoPago = () => {
    const [params] = useSearchParams();
    const reservaId = Number(params.get('reserva'));
    const idValido = Number.isInteger(reservaId) && reservaId > 0;
    const [reserva, setReserva] = useState<Reserva | null>(null);
    const [error, setError] = useState<string | null>(idValido ? null : 'No encontramos la reserva de este pago.');
    const [verificando, setVerificando] = useState(idValido);

    useEffect(() => {
        if (!idValido) return;
        let activo = true;
        verificarPago(reservaId)
            .then((actualizada) => {
                if (activo) setReserva(actualizada);
            })
            .catch((err: unknown) => {
                if (activo) setError(obtenerMensajeError(err, 'No pudimos consultar el estado de tu pago.'));
            })
            .finally(() => {
                if (activo) setVerificando(false);
            });
        return () => {
            activo = false;
        };
    }, [idValido, reservaId]);

    const handleVerificar = async () => {
        setError(null);
        setVerificando(true);
        try {
            setReserva(await verificarPago(reservaId));
        } catch (err) {
            setError(obtenerMensajeError(err, 'No pudimos consultar el estado de tu pago.'));
        } finally {
            setVerificando(false);
        }
    };

    const mensaje = reserva ? mensajePara(reserva) : null;

    return (
        <main className="reserva reserva--confirmacion">
            {verificando && !reserva && <p role="status">Consultando tu pago con Mercado Pago…</p>}
            {error && (
                <p role="alert" className="reserva__aviso reserva__aviso--error">
                    {error}
                </p>
            )}
            {reserva && mensaje && (
                <>
                    <h1>{mensaje.titulo}</h1>
                    <ResumenReserva reserva={reserva} />
                    <p role="status">{mensaje.texto}</p>
                    {mensaje.verificarDeNuevo && (
                        <button
                            type="button"
                            className="boton boton--naranja pago__boton"
                            onClick={handleVerificar}
                            disabled={verificando}
                        >
                            {verificando ? 'Verificando…' : 'Volver a verificar'}
                        </button>
                    )}
                    {mensaje.pagar && <BotonPagar reservaId={reserva.id} texto={mensaje.pagar} />}
                </>
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

export default ResultadoPago;
