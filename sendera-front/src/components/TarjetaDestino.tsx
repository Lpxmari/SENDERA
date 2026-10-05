import { Link } from 'react-router-dom';
import { Destino } from '../types/destino';

interface Props {
    destino: Destino;
}

const TarjetaDestino = ({ destino }: Props) => {
    // Usamos la primera foto del array o un placeholder si no hay
    const imagenPortada = destino.fotosUrl?.length > 0
        ? destino.fotosUrl[0]
        : 'https://via.placeholder.com/300x200?text=Sin+Imagen';

    return (
        <div className="tarjeta-destino" style={{ border: '1px solid #ccc', padding: '16px', borderRadius: '8px' }}>
            <img src={imagenPortada} alt={`Foto de ${destino.nombre}`} style={{ width: '100%', height: '200px', objectFit: 'cover' }} />
            <h3>{destino.nombre}</h3>
            <p>{destino.ubicacion}</p>
            <p><strong>Precio Base:</strong> ${destino.precioBase}</p>
            <Link to={`/destinos/${destino.id}`}>
                <button style={{ marginTop: '10px', padding: '8px 16px', cursor: 'pointer' }}>
                    Ver Detalles
                </button>
            </Link>
        </div>
    );
};

export default TarjetaDestino;