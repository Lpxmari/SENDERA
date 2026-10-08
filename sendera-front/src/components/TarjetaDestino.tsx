import { Link } from 'react-router-dom';
import { Destino } from '../types/destino';
import { formatearPrecio } from '../utils/formatearPrecio';

interface Props {
    destino: Destino;
    variante?: 'oferta' | 'popular';
}

const IconoUbicacion = () => (
    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
        <path d="M12 21s7-6.2 7-11.5A7 7 0 0 0 5 9.5C5 14.8 12 21 12 21Z" />
        <circle cx="12" cy="9.5" r="2.5" />
    </svg>
);

const TarjetaDestino = ({ destino, variante = 'oferta' }: Props) => {
    const imagenPortada = destino.fotosUrl?.length > 0 ? destino.fotosUrl[0] : null;

    return (
        <Link
            to={`/destinos/${destino.id}`}
            className={`tarjeta-destino tarjeta-destino--${variante}`}
        >
            <div className="tarjeta-destino__imagen">
                {imagenPortada ? (
                    <img src={imagenPortada} alt={`Foto de ${destino.nombre}`} loading="lazy" />
                ) : (
                    <span className="sin-imagen">Sin imagen</span>
                )}
            </div>

            {variante === 'oferta' ? (
                <div className="tarjeta-destino__cuerpo">
                    <h3>{destino.nombre}</h3>
                    <p className="tarjeta-destino__lugar">
                        <IconoUbicacion />
                        {destino.ubicacion}
                    </p>
                    <p className="tarjeta-destino__precio">
                        <span>Desde</span> {formatearPrecio(destino.precioBase)}
                    </p>
                </div>
            ) : (
                <div className="tarjeta-destino__cuerpo">
                    <h3>{destino.nombre}{destino.ubicacion ? ` , ${destino.ubicacion}` : ''}</h3>
                    <p className="tarjeta-destino__descripcion">{destino.descripcion}</p>
                </div>
            )}
        </Link>
    );
};

export default TarjetaDestino;