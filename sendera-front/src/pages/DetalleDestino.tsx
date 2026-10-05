import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { getDestinoPorId } from '../services/destinoService';
import { Destino } from '../types/destino';

const DetalleDestino = () => {
    const { id } = useParams<{ id: string }>();
    const [destino, setDestino] = useState<Destino | null>(null);
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const cargarDetalle = async () => {
            try {
                if (id) {
                    const data = await getDestinoPorId(Number(id));
                    setDestino(data);
                }
            } catch (err) {
                setError('Error al cargar los detalles del destino.');
            } finally {
                setLoading(false);
            }
        };
        cargarDetalle();
    }, [id]);

    if (loading) return <p>Cargando detalles...</p>;
    if (error || !destino) return <p style={{ color: 'red' }}>{error || 'Destino no encontrado'}</p>;

    return (
        <div style={{ maxWidth: '800px', margin: '0 auto', padding: '20px' }}>
            <h2>{destino.nombre}</h2>
            <p><strong>Ubicación:</strong> {destino.ubicacion}</p>
            <p><strong>Precio Base:</strong> ${destino.precioBase}</p>
            <p>{destino.descripcion}</p>

            <div style={{ display: 'flex', gap: '10px', marginTop: '20px' }}>
                <Link to={`/reservar/${destino.id}`}>
                    <button style={{ padding: '10px 20px', cursor: 'pointer', backgroundColor: '#0f5215', color: 'white', border: 'none', borderRadius: '4px' }}>
                        Reservar Ahora
                    </button>
                </Link>
                <Link to="/">
                    <button style={{ padding: '10px 20px', cursor: 'pointer', border: '1px solid #ccc', borderRadius: '4px' }}>Volver al Catálogo</button>
                </Link>
            </div>
        </div>
    );
};

export default DetalleDestino;