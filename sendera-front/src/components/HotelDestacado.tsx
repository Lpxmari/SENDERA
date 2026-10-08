import { Link } from 'react-router-dom';
import { Destino } from '../types/destino';
import { formatearPrecio } from '../utils/formatearPrecio';

interface Props {
    destino: Destino;
}

const HotelDestacado = ({ destino }: Props) => {
    const imagenPortada = destino.fotosUrl?.length > 0 ? destino.fotosUrl[0] : null;

    return (
        <article className="destacado">
            <h3 className="destacado__titulo">{destino.nombre}</h3>
            <div className="destacado__contenido">
                <div className="destacado__imagen">
                    {imagenPortada ? (
                        <img src={imagenPortada} alt={'Foto de ${destino.nombre}'} />
                    ) : (
                        <span className="sin-imagen">Sin imagen</span>
                    )}
                </div>
                <div className="destacado__info">
                    <h4>Acerca de</h4>
                    <p className="destacado__descripcion">{destino.descripcion}</p>
                    <ul className="destacado__datos">
                        <li>{destino.ubicacion}</li>
                        <li>Desde {formatearPrecio(destino.precioBase)}</li>
                    </ul>
                    <Link to={'/destinos/${destino.id}'} className="boton boton--naranja">
                        Ver detalles
                    </Link>
                </div>
            </div>
        </article>
    );
};

export default HotelDestacado;