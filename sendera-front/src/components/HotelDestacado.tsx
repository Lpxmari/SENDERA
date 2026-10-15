import { Link } from 'react-router-dom';
import { Destino } from '../types/destino';
import { serviciosAlojamiento } from '../utils/datosAlojamiento';
import { formatearPrecio } from '../utils/formatearPrecio';
import IconoServicio from './IconoServicio';

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
                        <img src={imagenPortada} alt={`Foto de ${destino.nombre}`} />
                    ) : (
                        <span className="sin-imagen">Sin imagen</span>
                    )}
                </div>
                <div className="destacado__info">
                    <h4>Acerca de</h4>
                    <p className="destacado__descripcion">{destino.descripcion}</p>
                    <ul className="destacado__datos" aria-label="Datos y servicios">
                        <li>
                            <IconoServicio id="ubicacion" />
                            {destino.ubicacion}
                        </li>
                        {destino.accesibilidad && (
                            <li>
                                <IconoServicio id="accesibilidad" />
                                Accesibilidad disponible
                            </li>
                        )}
                        {destino.dificultad && (
                            <li>
                                <IconoServicio id="dificultad" />
                                Dificultad {destino.dificultad.toLowerCase()}
                            </li>
                        )}
                        <li>
                            <IconoServicio id="precio" />
                            Desde {formatearPrecio(destino.precioBase)}
                        </li>
                        {/* Servicios simulados (datosAlojamiento): el backend aún no los expone. */}
                        {serviciosAlojamiento.map((servicio) => (
                            <li key={servicio.id}>
                                <IconoServicio id={servicio.id} />
                                {servicio.etiqueta}
                            </li>
                        ))}
                    </ul>
                    <Link to={`/destinos/${destino.id}`} className="boton boton--naranja">
                        Ver detalles
                    </Link>
                </div>
            </div>
        </article>
    );
};

export default HotelDestacado;