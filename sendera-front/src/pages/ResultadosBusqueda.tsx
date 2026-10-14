import { useEffect, useMemo, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { fetchDestinos } from '../services/destinoService';
import { Destino, DificultadEnum } from '../types/destino';
import { formatearPrecio } from '../utils/formatearPrecio';
import {
    Orden,
    calcularNoches,
    datosResultado,
    estrellasFiltro,
    opcionesOrden,
    rangosPresupuesto,
    serviciosFiltro,
} from '../utils/datosResultados';

const alternar = <T,>(lista: T[], valor: T): T[] =>
    lista.includes(valor) ? lista.filter((item) => item !== valor) : [...lista, valor];

const IconoCorazon = ({ lleno }: { lleno: boolean }) => (
    <svg viewBox="0 0 24 24" width="22" height="22" fill={lleno ? 'currentColor' : 'none'} stroke="currentColor" strokeWidth="1.8" strokeLinejoin="round" aria-hidden="true">
        <path d="M12 20.5s-7.5-4.6-9.2-9.4C1.6 7.6 3.7 4.5 7 4.5c2 0 3.9 1.1 5 3 1.1-1.9 3-3 5-3 3.3 0 5.4 3.1 4.2 6.6C19.5 15.9 12 20.5 12 20.5Z" />
    </svg>
);

const IconoDificultad = () => (
    <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path d="M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z" />
        <line x1="4" y1="22" x2="4" y2="15" />
    </svg>
);

const IconoAccesibilidad = () => (
    <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <circle cx="12" cy="4" r="2" />
        <path d="M12 6v6m0 0l-3 6m3-6l3 6M6 10h12" />
    </svg>
);

const opcionesDificultad: { id: DificultadEnum; etiqueta: string }[] = [
    { id: 'BAJA', etiqueta: 'Baja' },
    { id: 'MODERADA', etiqueta: 'Moderada' },
    { id: 'ALTA', etiqueta: 'Alta' },
];

const ResultadosBusqueda = () => {
    const [searchParams] = useSearchParams();
    const terminoOriginal = (searchParams.get('destino') ?? '').trim();
    const busqueda = terminoOriginal.toLowerCase();
    const llegada = searchParams.get('llegada') ?? '';
    const salida = searchParams.get('salida') ?? '';
    const noches = calcularNoches(llegada, salida);

    const [destinos, setDestinos] = useState<Destino[]>([]);
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);

    // Estados de filtros existentes
    const [presupuesto, setPresupuesto] = useState<string[]>([]);
    const [servicios, setServicios] = useState<string[]>([]);
    const [estrellas, setEstrellas] = useState<number[]>([]);
    
    // --- NUEVOS ESTADOS PROCESO 2 ---
    const [dificultades, setDificultades] = useState<DificultadEnum[]>([]);
    const [soloAccesibles, setSoloAccesibles] = useState<boolean>(false);

    const [orden, setOrden] = useState<Orden>('recomendados');
    const [guardados, setGuardados] = useState<number[]>([]);
    const [filtrosAbiertos, setFiltrosAbiertos] = useState<boolean>(false);

    useEffect(() => {
        const cargarDestinos = async () => {
            try {
                setDestinos(await fetchDestinos());
            } catch (err) {
                setError('No pudimos cargar los alojamientos. Verifica que el backend esté corriendo en el puerto 8080.');
            } finally {
                setLoading(false);
            }
        };
        cargarDestinos();
    }, []);

    const hayFiltros = presupuesto.length + servicios.length + estrellas.length + dificultades.length > 0 || soloAccesibles;

    const limpiar = () => {
        setPresupuesto([]);
        setServicios([]);
        setEstrellas([]);
        setDificultades([]);
        setSoloAccesibles(false);
    };

    const resultados = useMemo(() => {
        const rangosActivos = rangosPresupuesto.filter((r) => presupuesto.includes(r.id));

        const filtrados = destinos.filter((destino) => {
            if (busqueda && !`${destino.nombre} ${destino.ubicacion}`.toLowerCase().includes(busqueda)) return false;

            const datos = datosResultado(destino);
            if (rangosActivos.length > 0 && !rangosActivos.some((r) => destino.precioBase >= r.min && destino.precioBase <= r.max)) return false;
            if (!servicios.every((servicio) => datos.servicios.includes(servicio))) return false;
            if (estrellas.length > 0 && !estrellas.includes(datos.estrellas)) return false;

            // --- FILTRADO PROCESO 2 ---
            if (dificultades.length > 0 && !dificultades.includes(destino.dificultad)) return false;
            if (soloAccesibles && !destino.accesibilidad) return false;

            return true;
        });

        if (orden === 'precio-asc') return [...filtrados].sort((a, b) => a.precioBase - b.precioBase);
        if (orden === 'precio-desc') return [...filtrados].sort((a, b) => b.precioBase - a.precioBase);
        return filtrados;
    }, [destinos, busqueda, presupuesto, servicios, estrellas, dificultades, soloAccesibles, orden]);

    const titulo = `${terminoOriginal || 'Todos los destinos'}: ${resultados.length} ${resultados.length === 1 ? 'alojamiento' : 'alojamientos'}`;

    return (
        <div className="resultados">
            <aside className="filtros" aria-label="Filtros de búsqueda">
                <div className="filtros__cabecera">
                    <h2>Filtrar por</h2>
                    <button type="button" className="filtros__limpiar" onClick={limpiar} disabled={!hayFiltros}>
                        Limpiar
                    </button>
                </div>

                <button
                    type="button"
                    className="filtros__toggle"
                    aria-expanded={filtrosAbiertos}
                    onClick={() => setFiltrosAbiertos((abierto) => !abierto)}
                >
                    {filtrosAbiertos ? 'Ocultar filtros' : 'Mostrar filtros'}
                </button>

                <div className={`filtros__cuerpo ${filtrosAbiertos ? 'filtros__cuerpo--abierto' : ''}`}>
                    {/* --- NUEVO FILTRO: DIFICULTAD --- */}
                    <fieldset className="filtros__grupo">
                        <legend>Dificultad del sendero</legend>
                        {opcionesDificultad.map((dif) => (
                            <label key={dif.id}>
                                <input
                                    type="checkbox"
                                    checked={dificultades.includes(dif.id)}
                                    onChange={() => setDificultades((lista) => alternar(lista, dif.id))}
                                />
                                {dif.etiqueta}
                            </label>
                        ))}
                    </fieldset>

                    {/* --- NUEVO FILTRO: ACCESIBILIDAD --- */}
                    <fieldset className="filtros__grupo">
                        <legend>Accesibilidad</legend>
                        <label>
                            <input
                                type="checkbox"
                                checked={soloAccesibles}
                                onChange={(e) => setSoloAccesibles(e.target.checked)}
                            />
                            Solo accesibles
                        </label>
                    </fieldset>

                    <fieldset className="filtros__grupo">
                        <legend>Tu presupuesto</legend>
                        {rangosPresupuesto.map((rango) => (
                            <label key={rango.id}>
                                <input
                                    type="checkbox"
                                    checked={presupuesto.includes(rango.id)}
                                    onChange={() => setPresupuesto((lista) => alternar(lista, rango.id))}
                                />
                                {rango.etiqueta}
                            </label>
                        ))}
                    </fieldset>

                    <fieldset className="filtros__grupo">
                        <legend>Servicios</legend>
                        {serviciosFiltro.map((servicio) => (
                            <label key={servicio}>
                                <input
                                    type="checkbox"
                                    checked={servicios.includes(servicio)}
                                    onChange={() => setServicios((lista) => alternar(lista, servicio))}
                                />
                                {servicio}
                            </label>
                        ))}
                    </fieldset>

                    <fieldset className="filtros__grupo">
                        <legend>Categoría</legend>
                        {estrellasFiltro.map((cantidad) => (
                            <label key={cantidad}>
                                <input
                                    type="checkbox"
                                    checked={estrellas.includes(cantidad)}
                                    onChange={() => setEstrellas((lista) => alternar(lista, cantidad))}
                                />
                                {cantidad} estrellas
                            </label>
                        ))}
                    </fieldset>
                </div>
            </aside>

            <section className="resultados__principal" aria-labelledby="titulo-resultados">
                <nav className="detalle__migas" aria-label="Ruta de navegación">
                    <Link to="/">Inicio</Link>
                    <span aria-hidden="true"> / </span>
                    {terminoOriginal ? (
                        <>
                            <span>Colombia</span>
                            <span aria-hidden="true"> / </span>
                            <span aria-current="page">{terminoOriginal}</span>
                        </>
                    ) : (
                        <span aria-current="page">Resultados</span>
                    )}
                </nav>

                <div className="resultados__cabecera">
                    <div>
                        <h1 id="titulo-resultados" className="resultados__titulo">{titulo}</h1>
                        <p className="resultados__subtitulo">Opciones seleccionadas para tus fechas</p>
                    </div>
                    <label className="orden">
                        <span>Ordenar:</span>
                        <select value={orden} onChange={(e) => setOrden(e.target.value as Orden)}>
                            {opcionesOrden.map((opcion) => (
                                <option key={opcion.valor} value={opcion.valor}>
                                    {opcion.etiqueta}
                                </option>
                            ))}
                        </select>
                    </label>
                </div>

                {loading && <p className="estado">Cargando alojamientos…</p>}
                {error && <p className="estado estado--error" role="alert">{error}</p>}
                {!loading && !error && resultados.length === 0 && (
                    <div className="estado resultados__vacio">
                        <p>No encontramos alojamientos con esos criterios.</p>
                        {hayFiltros && (
                            <button type="button" className="boton boton--verde" onClick={limpiar}>
                                Quitar filtros
                            </button>
                        )}
                    </div>
                )}

                <ul className="resultados__lista">
                    {resultados.map((destino) => {
                        const datos = datosResultado(destino);
                        const estaGuardado = guardados.includes(destino.id);
                        const portada = destino.fotosUrl?.length > 0 ? destino.fotosUrl[0] : null;

                        return (
                            <li key={destino.id} className="resultado-card">
                                <Link to={`/destinos/${destino.id}?${searchParams.toString()}`} className="resultado-card__imagen" aria-label={`Ver ${destino.nombre}`} tabIndex={-1}>
                                    {portada ? (
                                        <img src={portada} alt={`Foto de ${destino.nombre}`} loading="lazy" />
                                    ) : (
                                        <span className="sin-imagen">Sin imagen</span>
                                    )}
                                </Link>

                                <div className="resultado-card__cuerpo">
                                    <h3 className="resultado-card__nombre">
                                        <Link to={`/destinos/${destino.id}?${searchParams.toString()}`}>{destino.nombre}</Link>
                                    </h3>
                                    <p className="resultado-card__zona">{destino.ubicacion}</p>
                                    <p className="resultado-card__descripcion">{destino.descripcion}</p>

                                    {/* --- ETIQUETAS BADGES MOSTRADAS EN LA TARJETA --- */}
                                    <div className="resultado-card__atributos" style={{ display: 'flex', gap: '8px', marginTop: '6px' }}>
                                        {destino.dificultad && (
                                            <span className={`badge badge--dificultad-${destino.dificultad.toLowerCase()}`}>
                                                <IconoDificultad />
                                                {destino.dificultad}
                                            </span>
                                        )}
                                        {destino.accesibilidad && (
                                            <span className="badge badge--accesible">
                                                <IconoAccesibilidad />
                                                Accesible
                                            </span>
                                        )}
                                    </div>

                                    <span className={`insignia ${datos.insignia === 'Oferta de temporada' ? 'insignia--oferta' : ''}`}>
                                        {datos.insignia}
                                    </span>

                                    <div className="resultado-card__pie">
                                        <p className="resultado-card__nota">
                                            <span className="calificacion__puntaje">{String(datos.puntaje).replace('.', ',')}</span>
                                            {datos.etiquetaPuntaje} · {datos.resenas} reseñas
                                        </p>
                                        <div className="resultado-card__precio">
                                            <s aria-label="Precio anterior">{formatearPrecio(datos.precioAnterior)}</s>
                                            <strong>{formatearPrecio(destino.precioBase)}</strong>
                                            <small>por noche · impuestos incluidos</small>
                                            {noches > 0 && (
                                                <small>
                                                    {noches} {noches === 1 ? 'noche' : 'noches'}: {formatearPrecio(destino.precioBase * noches)}
                                                </small>
                                            )}
                                        </div>
                                    </div>
                                </div>

                                <button
                                    type="button"
                                    className={`resultado-card__guardar ${estaGuardado ? 'resultado-card__guardar--activo' : ''}`}
                                    aria-pressed={estaGuardado}
                                    aria-label={estaGuardado ? `Quitar ${destino.nombre} de guardados` : `Guardar ${destino.nombre}`}
                                    onClick={() => setGuardados((lista) => alternar(lista, destino.id))}
                                >
                                    <IconoCorazon lleno={estaGuardado} />
                                </button>
                            </li>
                        );
                    })}
                </ul>
            </section>
        </div>
    );
};

export default ResultadosBusqueda;