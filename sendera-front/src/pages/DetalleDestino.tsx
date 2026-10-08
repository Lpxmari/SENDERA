import { useEffect, useMemo, useState } from 'react';
import { useParams, useSearchParams, Link } from 'react-router-dom';
import { getDestinoPorId } from '../services/destinoService';
import { Destino } from '../types/destino';
import { formatearPrecio } from '../utils/formatearPrecio';
import {
    calificacionAlojamiento,
    categoriaAlojamiento,
    eslogan,
    estrellasAlojamiento,
    opinionesAlojamiento,
    serviciosAlojamiento,
} from '../utils/datosAlojamiento';

const MS_POR_DIA = 1000 * 60 * 60 * 24;

const calcularNoches = (llegada: string, salida: string): number => {
    if (!llegada || !salida) return 0;
    const diferencia = (new Date(salida).getTime() - new Date(llegada).getTime()) / MS_POR_DIA;
    return Number.isFinite(diferencia) && diferencia > 0 ? Math.round(diferencia) : 0;
};

const IconoServicio = ({ id }: { id: string }) => {
    const props = {
        viewBox: '0 0 24 24',
        width: 18,
        height: 18,
        fill: 'none',
        stroke: 'currentColor',
        strokeWidth: 1.8,
        strokeLinecap: 'round' as const,
        strokeLinejoin: 'round' as const,
        'aria-hidden': true,
    };
    switch (id) {
        case 'piscina':
            return (
                <svg {...props}>
                    <path d="M3 17c2 0 2-1.5 4.5-1.5S10 17 12 17s2.5-1.5 4.5-1.5S19 17 21 17" />
                    <path d="M3 21c2 0 2-1.5 4.5-1.5S10 21 12 21s2.5-1.5 4.5-1.5S19 21 21 21" />
                    <path d="M8 13V6a2 2 0 0 1 4 0M16 13V6a2 2 0 0 0-4 0M8 9h8" />
                </svg>
            );
        case 'desayuno':
            return (
                <svg {...props}>
                    <path d="M4 9h13v5a5 5 0 0 1-5 5H9a5 5 0 0 1-5-5V9Z" />
                    <path d="M17 10h1.5a2.5 2.5 0 0 1 0 5H17" />
                    <path d="M8 3v3M12 3v3" />
                </svg>
            );
        case 'wifi':
            return (
                <svg {...props}>
                    <path d="M2 9a15 15 0 0 1 20 0M5 12.5a10 10 0 0 1 14 0M8.5 16a5 5 0 0 1 7 0" />
                    <circle cx="12" cy="19" r="1" />
                </svg>
            );
        default:
            return (
                <svg {...props}>
                    <circle cx="12" cy="12" r="9" />
                    <path d="M9.5 16.5V8h3.2a2.6 2.6 0 0 1 0 5.2H9.5" />
                </svg>
            );
    }
};

const IconoPerfil = () => (
    <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" aria-hidden="true">
        <circle cx="12" cy="8" r="4" />
        <path d="M4 21c0-4.4 3.6-7 8-7s8 2.6 8 7" />
    </svg>
);

const DetalleDestino = () => {
    const { id } = useParams<{ id: string }>();
    const [searchParams] = useSearchParams();

    const [destino, setDestino] = useState<Destino | null>(null);
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);
    const [fotoActiva, setFotoActiva] = useState<number>(0);
    const [guardado, setGuardado] = useState<boolean>(false);
    const [mensaje, setMensaje] = useState<string>('');

    const [llegada, setLlegada] = useState<string>(searchParams.get('llegada') ?? '');
    const [salida, setSalida] = useState<string>(searchParams.get('salida') ?? '');
    const viajeros = Number(searchParams.get('viajeros') ?? '2') || 2;

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

    const noches = useMemo(() => calcularNoches(llegada, salida), [llegada, salida]);

    if (loading) return <p className="estado detalle__estado">Cargando detalles…</p>;
    if (error || !destino) {
        return (
            <p className="estado estado--error detalle__estado" role="alert">
                {error || 'Destino no encontrado'}
            </p>
        );
    }

    const fotos = destino.fotosUrl ?? [];
    const fotoPrincipal = fotos[fotoActiva] ?? null;
    // Las miniaturas son las demás fotos (sin repetir la principal)
    const miniaturas = fotos
        .map((url, indice) => ({ url, indice }))
        .filter(({ indice }) => indice !== fotoActiva)
        .slice(0, 2);

    const total = noches > 0 ? destino.precioBase * noches : 0;

    const parametrosReserva = new URLSearchParams();
    if (llegada) parametrosReserva.set('llegada', llegada);
    if (salida) parametrosReserva.set('salida', salida);
    parametrosReserva.set('viajeros', String(viajeros));
    const enlaceReserva = `/reservar/${destino.id}?${parametrosReserva.toString()}`;

    const compartir = async () => {
        const url = window.location.href;
        try {
            if (navigator.share) {
                await navigator.share({ title: destino.nombre, url });
            } else {
                await navigator.clipboard.writeText(url);
                setMensaje('Enlace copiado');
                setTimeout(() => setMensaje(''), 2500);
            }
        } catch {
            /* el usuario canceló: no hacemos nada */
        }
    };

    return (
        <div className="detalle">
            <nav className="detalle__migas" aria-label="Ruta de navegación">
                <Link to="/resultados">Resultados</Link>
                <span aria-hidden="true"> / </span>
                {destino.ubicacion && (
                    <>
                        <span>{destino.ubicacion}</span>
                        <span aria-hidden="true"> / </span>
                    </>
                )}
                <span aria-current="page">{destino.nombre}</span>
            </nav>

            <header className="detalle__cabecera">
                <div>
                    <p className="detalle__categoria">
                        {categoriaAlojamiento}
                        <span className="detalle__estrellas" role="img" aria-label={`${estrellasAlojamiento} estrellas`}>
                            {'★'.repeat(estrellasAlojamiento)}
                        </span>
                    </p>
                    <h1 className="detalle__titulo">{destino.nombre}</h1>
                    <p className="detalle__direccion">{destino.ubicacion}</p>
                </div>
                <div className="detalle__acciones">
                    <button type="button" className="boton-contorno" onClick={compartir}>
                        Compartir
                    </button>
                    <button
                        type="button"
                        className={`boton-contorno ${guardado ? 'boton-contorno--activo' : ''}`}
                        aria-pressed={guardado}
                        onClick={() => setGuardado((g) => !g)}
                    >
                        {guardado ? 'Guardado' : 'Guardar'}
                    </button>
                </div>
            </header>
            <p className="detalle__aviso" role="status" aria-live="polite">{mensaje}</p>

            <section className="galeria" aria-label="Fotos del alojamiento">
                <div className="galeria__principal">
                    {fotoPrincipal ? (
                        <img src={fotoPrincipal} alt={`Foto de ${destino.nombre}`} />
                    ) : (
                        <span className="sin-imagen">Sin imagen</span>
                    )}
                </div>
                {[0, 1].map((posicion) => {
                    const miniatura = miniaturas[posicion];
                    return (
                        <div key={posicion} className={`galeria__mini galeria__mini--${posicion + 1}`}>
                            {miniatura ? (
                                <button
                                    type="button"
                                    onClick={() => setFotoActiva(miniatura.indice)}
                                    aria-label={`Ver foto ${miniatura.indice + 1} de ${destino.nombre}`}
                                >
                                    <img src={miniatura.url} alt="" loading="lazy" />
                                </button>
                            ) : (
                                <span className="sin-imagen">Sin imagen</span>
                            )}
                        </div>
                    );
                })}
            </section>

            <div className="detalle__cuerpo">
                <section className="detalle__info" aria-labelledby="detalle-eslogan">
                    <div className="calificacion">
                        <span className="calificacion__puntaje">{calificacionAlojamiento.puntaje}</span>
                        <span className="calificacion__texto">
                            {calificacionAlojamiento.etiqueta} · {calificacionAlojamiento.totalResenas} reseñas
                        </span>
                    </div>

                    <h2 id="detalle-eslogan" className="detalle__eslogan">{eslogan}</h2>
                    <p className="detalle__descripcion">{destino.descripcion}</p>

                    <ul className="servicios-lista" aria-label="Servicios incluidos">
                        {serviciosAlojamiento.map((servicio) => (
                            <li key={servicio.id}>
                                <IconoServicio id={servicio.id} />
                                {servicio.etiqueta}
                            </li>
                        ))}
                    </ul>
                </section>

                <aside className="reserva-card" aria-label="Resumen de reserva">
                    <p className="reserva-card__precio">
                        {formatearPrecio(destino.precioBase)} <span>/ noche</span>
                    </p>

                    <div className="reserva-card__fechas">
                        <label>
                            <span>Llegada</span>
                            <input type="date" value={llegada} onChange={(e) => setLlegada(e.target.value)} />
                        </label>
                        <label>
                            <span>Salida</span>
                            <input type="date" value={salida} min={llegada} onChange={(e) => setSalida(e.target.value)} />
                        </label>
                    </div>

                    <p className="reserva-card__detalle">
                        {viajeros} {viajeros === 1 ? 'adulto' : 'adultos'} · 1 habitación
                    </p>

                    {total > 0 && (
                        <p className="reserva-card__total">
                            {noches} {noches === 1 ? 'noche' : 'noches'}: <strong>{formatearPrecio(total)}</strong>
                        </p>
                    )}

                    <Link to={enlaceReserva} className="boton boton--naranja boton--grande">
                        Reservar ahora
                    </Link>
                    <p className="reserva-card__nota">No se hará ningún cargo todavía</p>
                </aside>
            </div>

            <section className="opiniones" aria-labelledby="titulo-opiniones">
                <h2 id="titulo-opiniones" className="titulo-seccion">Mira algunas de las opiniones</h2>
                <div className="opiniones__rejilla">
                    {opinionesAlojamiento.map((opinion) => (
                        <figure key={opinion.id} className="opinion">
                            <blockquote>“{opinion.texto}”</blockquote>
                            <figcaption>
                                <IconoPerfil />
                                <span>
                                    <strong>{opinion.autor}</strong>
                                    {opinion.perfil}
                                </span>
                            </figcaption>
                        </figure>
                    ))}
                </div>
            </section>
        </div>
    );
};

export default DetalleDestino;