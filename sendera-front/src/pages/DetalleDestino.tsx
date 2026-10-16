import { ChangeEvent, FormEvent, useEffect, useMemo, useRef, useState } from 'react';
import { useParams, useSearchParams, Link, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { obtenerMensajeError } from '../services/apiClient';
import { getDestinoPorId } from '../services/destinoService';
import { crearResena, listarResenas, obtenerUrlImagenResena } from '../services/resenaService';
import { Destino } from '../types/destino';
import { Resena } from '../types/resena';
import { formatearPrecio } from '../utils/formatearPrecio';
import {
    categoriaAlojamiento,
    eslogan,
    estrellasAlojamiento,
    serviciosAlojamiento,
} from '../utils/datosAlojamiento';

const MS_POR_DIA = 1000 * 60 * 60 * 24;
const TAMANO_MAXIMO_IMAGEN = 5 * 1024 * 1024;
const TIPOS_IMAGEN_PERMITIDOS = ['image/jpeg', 'image/png', 'image/webp'];

interface EstadoResenas {
    destinoId: string | null;
    datos: Resena[];
    cargando: boolean;
    error: string | null;
}

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
    const ubicacion = useLocation();
    const { autenticado } = useAuth();

    const [destino, setDestino] = useState<Destino | null>(null);
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);
    const [fotoActiva, setFotoActiva] = useState<number>(0);
    const [guardado, setGuardado] = useState<boolean>(false);
    const [mensaje, setMensaje] = useState<string>('');
    const [estadoResenas, setEstadoResenas] = useState<EstadoResenas>({
        destinoId: null,
        datos: [],
        cargando: true,
        error: null,
    });
    const [comentario, setComentario] = useState('');
    const [calificacion, setCalificacion] = useState(0);
    const [imagen, setImagen] = useState<File | null>(null);
    const [vistaPrevia, setVistaPrevia] = useState<string | null>(null);
    const [publicandoResena, setPublicandoResena] = useState(false);
    const [errorFormulario, setErrorFormulario] = useState<string | null>(null);
    const referenciaImagen = useRef<HTMLInputElement>(null);
    const lectorImagen = useRef<FileReader | null>(null);

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
                setError(obtenerMensajeError(err, 'Error al cargar los detalles del destino.'));
            } finally {
                setLoading(false);
            }
        };
        cargarDetalle();
    }, [id]);

    useEffect(() => {
        if (!id) return;
        let cancelado = false;
        listarResenas(Number(id))
            .then((datos) => {
                if (!cancelado) setEstadoResenas({ destinoId: id, datos, cargando: false, error: null });
            })
            .catch((err: unknown) => {
                if (!cancelado) {
                    setEstadoResenas({
                        destinoId: id,
                        datos: [],
                        cargando: false,
                        error: obtenerMensajeError(err, 'No se pudieron cargar las reseñas.'),
                    });
                }
            })
        return () => {
            cancelado = true;
        };
    }, [id]);

    useEffect(() => {
        return () => lectorImagen.current?.abort();
    }, []);

    const noches = useMemo(() => calcularNoches(llegada, salida), [llegada, salida]);
    const estadoResenasActual = estadoResenas.destinoId === id ? estadoResenas : null;
    const resenas = estadoResenasActual?.datos ?? [];
    const cargandoResenas = estadoResenasActual?.cargando ?? true;
    const errorResenas = estadoResenasActual?.error ?? null;

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
    const promedioCalificacion = resenas.length
        ? (resenas.reduce((totalCalificaciones, resena) => totalCalificaciones + resena.calificacion, 0) / resenas.length).toFixed(1)
        : null;

    const seleccionarImagen = (evento: ChangeEvent<HTMLInputElement>) => {
        const archivo = evento.target.files?.[0] ?? null;
        setErrorFormulario(null);
        lectorImagen.current?.abort();
        if (archivo && !TIPOS_IMAGEN_PERMITIDOS.includes(archivo.type)) {
            setErrorFormulario('La imagen debe ser JPEG, PNG o WebP.');
            evento.target.value = '';
            setImagen(null);
            setVistaPrevia(null);
            return;
        }
        if (archivo && archivo.size > TAMANO_MAXIMO_IMAGEN) {
            setErrorFormulario('La imagen no puede superar los 5 MB.');
            evento.target.value = '';
            setImagen(null);
            setVistaPrevia(null);
            return;
        }
        setImagen(archivo);
        setVistaPrevia(null);
        if (archivo) {
            const lector = new FileReader();
            lectorImagen.current = lector;
            lector.onload = () => {
                if (typeof lector.result === 'string') setVistaPrevia(lector.result);
            };
            lector.onerror = () => setErrorFormulario('No se pudo leer la imagen seleccionada.');
            lector.readAsDataURL(archivo);
        }
    };

    const publicarResena = async (evento: FormEvent<HTMLFormElement>) => {
        evento.preventDefault();
        if (!id || !comentario.trim() || calificacion < 1 || calificacion > 5) {
            setErrorFormulario('Escribe tu reseña y selecciona una calificación de 1 a 5 estrellas.');
            return;
        }

        setPublicandoResena(true);
        setErrorFormulario(null);
        try {
            const nuevaResena = await crearResena(
                Number(id),
                { comentario: comentario.trim(), calificacion },
                imagen,
            );
            setEstadoResenas((actual) => actual.destinoId === id
                ? { ...actual, datos: [nuevaResena, ...actual.datos] }
                : actual);
            setComentario('');
            setCalificacion(0);
            setImagen(null);
            setVistaPrevia(null);
            lectorImagen.current?.abort();
            if (referenciaImagen.current) referenciaImagen.current.value = '';
        } catch (err) {
            setErrorFormulario(obtenerMensajeError(err, 'No se pudo publicar la reseña. Intenta de nuevo.'));
        } finally {
            setPublicandoResena(false);
        }
    };

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
                        {promedioCalificacion && <span className="calificacion__puntaje">{promedioCalificacion}/5</span>}
                        <span className="calificacion__texto">
                            {cargandoResenas
                                ? 'Cargando calificaciones…'
                                : errorResenas
                                    ? 'Calificaciones no disponibles'
                                    : resenas.length
                                ? `${resenas.length} ${resenas.length === 1 ? 'reseña' : 'reseñas'}`
                                : 'Aún no hay reseñas'}
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
                <h2 id="titulo-opiniones" className="titulo-seccion">Reseñas de este destino</h2>
                {autenticado ? (
                    <form className="resena-form" onSubmit={publicarResena}>
                        <h3>Comparte tu experiencia</h3>
                        <label className="resena-form__campo">
                            <span>Tu reseña</span>
                            <textarea
                                value={comentario}
                                onChange={(evento) => setComentario(evento.target.value)}
                                maxLength={2000}
                                rows={4}
                                required
                                placeholder="¿Qué te pareció este destino?"
                            />
                            <small>{comentario.length}/2000 caracteres</small>
                        </label>
                        <fieldset className="resena-form__calificacion">
                            <legend>Tu calificación</legend>
                            <div className="resena-form__estrellas">
                                {[1, 2, 3, 4, 5].map((estrella) => (
                                    <label
                                        key={estrella}
                                        className={estrella <= calificacion ? 'resena-form__estrella resena-form__estrella--activa' : 'resena-form__estrella'}
                                    >
                                        <input
                                            type="radio"
                                            name="calificacion"
                                            value={estrella}
                                            checked={calificacion === estrella}
                                            onChange={() => setCalificacion(estrella)}
                                            required
                                        />
                                        <span aria-hidden="true">★</span>
                                        <span className="solo-lectores">{estrella} {estrella === 1 ? 'estrella' : 'estrellas'}</span>
                                    </label>
                                ))}
                            </div>
                        </fieldset>
                        <label className="resena-form__campo">
                            <span>Foto (opcional)</span>
                            <input
                                ref={referenciaImagen}
                                type="file"
                                accept="image/jpeg,image/png,image/webp"
                                onChange={seleccionarImagen}
                            />
                        </label>
                        {vistaPrevia && (
                            <div className="resena-form__vista-previa">
                                <img src={vistaPrevia} alt="Vista previa de la imagen adjunta" />
                                <button
                                    type="button"
                                    className="boton-contorno"
                                    onClick={() => {
                                        setImagen(null);
                                        setVistaPrevia(null);
                                        lectorImagen.current?.abort();
                                        if (referenciaImagen.current) referenciaImagen.current.value = '';
                                    }}
                                >
                                    Quitar imagen
                                </button>
                            </div>
                        )}
                        {errorFormulario && <p className="resena-form__error" role="alert">{errorFormulario}</p>}
                        <button type="submit" className="boton boton--verde" disabled={publicandoResena}>
                            {publicandoResena ? 'Publicando…' : 'Publicar reseña'}
                        </button>
                    </form>
                ) : (
                    <p className="opiniones__invitacion">
                        <Link
                            to="/login"
                            state={{ desde: ubicacion.pathname + ubicacion.search }}
                            className="boton boton--verde"
                        >
                            Inicia sesión para escribir una reseña
                        </Link>
                    </p>
                )}

                {errorResenas && <p className="estado estado--error" role="alert">{errorResenas}</p>}
                {cargandoResenas && <p className="estado" role="status">Cargando reseñas…</p>}
                {!cargandoResenas && !errorResenas && resenas.length === 0 && (
                    <p className="opiniones__vacias">Sé la primera persona en compartir su experiencia.</p>
                )}
                <div className="opiniones__rejilla">
                    {resenas.map((resena) => (
                        <figure key={resena.id} className="opinion">
                            <div className="opinion__contenido">
                                <p className="opinion__estrellas" role="img" aria-label={`${resena.calificacion} de 5 estrellas`}>
                                    {'★'.repeat(resena.calificacion)}{'☆'.repeat(5 - resena.calificacion)}
                                </p>
                                <blockquote>{resena.comentario}</blockquote>
                                {resena.imagenUrl && (
                                    <img
                                        className="opinion__imagen"
                                        src={obtenerUrlImagenResena(resena.imagenUrl)}
                                        alt={`Imagen adjunta a la reseña de ${resena.autor}`}
                                        loading="lazy"
                                    />
                                )}
                            </div>
                            <figcaption>
                                <IconoPerfil />
                                <span>
                                    <strong>{resena.autor}</strong>
                                    {new Date(resena.creadaEn).toLocaleDateString()}
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