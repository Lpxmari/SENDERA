import { useEffect, useState } from 'react';
import { fetchDestinos } from '../services/destinoService';
import { Destino } from '../types/destino';
import TarjetaDestino from '../components/TarjetaDestino';

const Home = () => {
    const [destinos, setDestinos] = useState<Destino[]>([]);
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const cargarDestinos = async () => {
            try {
                const data = await fetchDestinos();
                setDestinos(data);
            } catch (err) {
                setError('Error al cargar los destinos. Verifica que el backend esté corriendo en el puerto 8080.');
            } finally {
                setLoading(false);
            }
        };

        cargarDestinos();
    }, []);

    if (loading) return <p>Cargando destinos...</p>;
    if (error) return <p style={{ color: 'red' }}>{error}</p>;

    return (
        <div>
            <h2>Explora nuestros destinos turísticos</h2>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(250px, 1fr))', gap: '20px' }}>
                {destinos.map((destino) => (
                    <TarjetaDestino key={destino.id} destino={destino} />
                ))}
            </div>
        </div>
    );
};

export default Home;