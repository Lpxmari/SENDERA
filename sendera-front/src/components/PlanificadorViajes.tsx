import { useState } from 'react';
import { Link } from 'react-router-dom';
import { CategoriaViaje } from '../types/planificador';
import { categoriasViaje, sugerenciasViaje } from '../utils/sugerenciasViaje';
import IconoUbicacion from './IconoUbicacion';

const PlanificadorViajes = () => {
    const [categoria, setCategoria] = useState<CategoriaViaje | null>(null);

    const sugerencias = categoria ? sugerenciasViaje.filter((s) => s.categoria === categoria) : sugerenciasViaje;

    const handleClickCategoria = (seleccionada: CategoriaViaje) => {
        setCategoria((actual) => (actual === seleccionada ? null : seleccionada));
    };

    return (
        <section className="planificador" aria-labelledby="titulo-planificador">
            <h2 id="titulo-planificador" className="titulo-seccion titulo-seccion--naranja">
                Planificador de viajes rápido y sencillo
            </h2>
            <p className="planificador__intro">Elige un estilo y descubre los principales destinos en tu País</p>

            <div className="planificador__categorias" role="group" aria-label="Estilo de viaje">
                {categoriasViaje.map((c) => (
                    <button
                        key={c}
                        type="button"
                        className={`categoria ${categoria === c ? 'categoria--activa' : ''}`}
                        aria-pressed={categoria === c}
                        onClick={() => handleClickCategoria(c)}
                    >
                        {c}
                    </button>
                ))}
            </div>

            <ul className="planificador__rejilla">
                {sugerencias.map((s) => (
                    <li key={s.id} className="sugerencia">
                        <Link
                            to={`/destinos/${s.destinoId}`}
                            className="sugerencia__enlace"
                            aria-label={`Ver el detalle de ${s.nombre}, ${s.lugar}`}
                        >
                            <div className="sugerencia__imagen">
                                {s.imagenUrl ? (
                                    <img src={s.imagenUrl} alt="" loading="lazy" />
                                ) : (
                                    <span className="sin-imagen">Sin imagen</span>
                                )}
                            </div>
                            <h3>{s.nombre}</h3>
                            <p className="sugerencia__tipo">{s.tipo}</p>
                            <p className="sugerencia__lugar">
                                <IconoUbicacion />
                                {s.lugar}
                            </p>
                        </Link>
                    </li>
                ))}
            </ul>
        </section>
    );
};

export default PlanificadorViajes;
