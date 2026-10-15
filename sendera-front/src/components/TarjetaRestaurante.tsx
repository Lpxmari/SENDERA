import { Restaurante } from '../types/restaurante';
import { formatearPrecio } from '../utils/formatearPrecio';

interface Props {
    restaurante: Restaurante;
}

const TarjetaRestaurante = ({ restaurante }: Props) => (
    <article className="tarjeta-destino tarjeta-destino--oferta tarjeta-restaurante">
        <div className="tarjeta-destino__imagen">
            {restaurante.fotoUrl ? (
                <img src={restaurante.fotoUrl} alt={`Foto de ${restaurante.nombre}`} loading="lazy" />
            ) : (
                <span className="sin-imagen">Sin imagen</span>
            )}
        </div>

        <div className="tarjeta-destino__cuerpo">
            <h3>{restaurante.nombre}</h3>
            <p className="tarjeta-destino__lugar">
                {restaurante.tipoCocina} · {restaurante.ubicacion}
            </p>
            <p className="tarjeta-destino__descripcion">{restaurante.descripcion}</p>

            <div className="tarjeta-restaurante__datos">
                <span className="tarjeta-restaurante__calificacion">
                    <span aria-hidden="true">★</span> {restaurante.calificacion.toFixed(1)}
                    <span className="sr-only"> de 5</span>
                </span>
                {restaurante.accesible && <span className="tarjeta-restaurante__accesible">Accesible</span>}
            </div>

            <p className="tarjeta-destino__precio">
                <span>Promedio por persona</span> {formatearPrecio(restaurante.precioPromedio)}
            </p>
        </div>
    </article>
);

export default TarjetaRestaurante;
