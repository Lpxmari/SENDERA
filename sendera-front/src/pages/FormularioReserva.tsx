import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { crearReserva } from '../services/reservaService';
import { Reserva } from '../types/reserva';

const FormularioReserva = () => {
    const { destinoId } = useParams<{ destinoId: string }>();
    const navigate = useNavigate();
    const [fechaReserva, setFechaReserva] = useState('');
    const [error, setError] = useState<string | null>(null);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            const nuevaReserva: Omit<Reserva, 'id'> = {
                itinerarioId: Number(destinoId), // Relacionamos la reserva con el destino actual
                fechaReserva: fechaReserva,
                montoTotal: 0, // En un escenario real, esto se calcula en base al precio del destino o se calcula en el backend
                estadoPago: 'PENDIENTE',
            };

            await crearReserva(nuevaReserva);
            navigate('/confirmacion'); // Redirección tras un 201 Created
        } catch (err) {
            setError('Error al conectar con el servidor para crear la reserva.');
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