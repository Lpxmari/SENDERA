import { useEffect, useState } from 'react';
import { obtenerMensajeError } from '../services/apiClient';
import { iniciarPago } from '../services/reservaService';

interface BotonPagarProps {
    reservaId: number;
    texto?: string;
    /** Texto mientras se abre Mercado Pago; corto en espacios estrechos como el historial. */
    textoAbriendo?: string;
    /** Nombre accesible cuando hay varios botones iguales (p. ej. en el historial). */
    etiqueta?: string;
}

/** Lleva al Checkout Pro de Mercado Pago: los datos de la tarjeta se escriben allí, nunca en SENDERA. */
const BotonPagar = ({
    reservaId,
    texto = 'Pagar con Mercado Pago',
    textoAbriendo = 'Abriendo Mercado Pago…',
    etiqueta,
}: BotonPagarProps) => {
    const [abriendo, setAbriendo] = useState(false);
    const [error, setError] = useState<string | null>(null);

    // Al volver con "Atrás" el navegador puede restaurar la página tal como quedó (bfcache): se reactiva el botón.
    useEffect(() => {
        const handleVolver = (evento: PageTransitionEvent) => {
            if (evento.persisted) setAbriendo(false);
        };
        window.addEventListener('pageshow', handleVolver);
        return () => window.removeEventListener('pageshow', handleVolver);
    }, []);

    const handlePagar = async () => {
        setError(null);
        setAbriendo(true);
        try {
            const { urlPago } = await iniciarPago(reservaId);
            window.location.assign(urlPago); // Se queda "abriendo" mientras el navegador sale hacia Mercado Pago
        } catch (err) {
            setError(obtenerMensajeError(err, 'No pudimos abrir el pago. Intenta de nuevo.'));
            setAbriendo(false);
        }
    };

    return (
        <div className="pago">
            <button
                type="button"
                className="boton boton--verde pago__boton"
                onClick={handlePagar}
                disabled={abriendo}
                aria-label={etiqueta}
            >
                {abriendo ? textoAbriendo : texto}
            </button>
            {error && (
                <p role="alert" className="reserva__aviso reserva__aviso--error">
                    {error}
                </p>
            )}
        </div>
    );
};

export default BotonPagar;
