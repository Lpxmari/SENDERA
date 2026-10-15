import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { crearReserva } from '../services/reservaService';
import { obtenerMensajeError } from '../services/apiClient';
import { CrearReservaRequest } from '../types/reserva';

const FormularioReserva = () => {
    const { destinoId } = useParams<{ destinoId: string }>();
    const navigate = useNavigate();
    const [fechaReserva, setFechaReserva] = useState('');
    const [error, setError] = useState<string | null>(null);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            // PENDIENTE: la API aún no ofrece itinerarios por destino, así que el id del destino se envía como
            // itinerarioId. Es incorrecto semánticamente y debe resolverse con un endpoint de itinerarios.
            const nuevaReserva: CrearReservaRequest = {
                itinerarioId: Number(destinoId),
                fechaReserva: fechaReserva,
            };

            await crearReserva(nuevaReserva);
            navigate('/confirmacion'); // Redirección tras un 201 Created
        } catch (err) {
            setError(obtenerMensajeError(err, 'No se pudo crear la reserva. Intenta de nuevo.'));
        }
    };

    return (
        <div style={{ maxWidth: '400px', margin: '20px auto', padding: '20px', border: '1px solid #ddd', borderRadius: '8px' }}>
            <h2>Crear Reserva</h2>
            {error && <p style={{ color: 'red', fontSize: '14px' }}>{error}</p>}
            <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
                <label style={{ display: 'flex', flexDirection: 'column', gap: '5px' }}>
                    Fecha de Reserva:
                    <input
                        type="date"
                        value={fechaReserva}
                        onChange={(e) => setFechaReserva(e.target.value)}
                        required
                        style={{ padding: '8px', borderRadius: '4px', border: '1px solid #ccc' }}
                    />
                </label>
                <button type="submit" style={{ padding: '10px', backgroundColor: '#008CBA', color: 'white', cursor: 'pointer', border: 'none', borderRadius: '4px' }}>
                    Confirmar Reserva
                </button>
            </form>
        </div>
    );
};

export default FormularioReserva;