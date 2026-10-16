import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { obtenerMensajeError } from '../services/apiClient';
import { fetchDestinos } from '../services/destinoService';
import { Destino } from '../types/destino';
import { Restaurante } from '../types/restaurante';
import { fetchRestaurantes } from '../services/restauranteService';
import TarjetaRestaurante from '../components/TarjetaRestaurante';
import TarjetaDestino from '../components/TarjetaDestino';
import HotelDestacado from '../components/HotelDestacado';
import PlanificadorViajes from '../components/PlanificadorViajes';

type TipoServicio = 'alojamiento' | 'restaurantes';

const Home = () => {
    const [destinos, setDestinos] = useState<Destino[]>([]);
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);
    const [restaurantes, setRestaurantes] = useState<Restaurante[] | null>(null);
    const [errorRestaurantes, setErrorRestaurantes] = useState<string | null>(null);
    const [servicio, setServicio] = useState<TipoServicio>('alojamiento');
    const [searchParams] = useSearchParams();
    const busqueda = (searchParams.get('destino') ?? '').trim().toLowerCase();

    useEffect(() => {
        const cargarDestinos = async () => {
            try {
                const data = await fetchDestinos();
                setDestinos(data);
            } catch (err) {
                setError(obtenerMensajeError(err, 'No pudimos cargar los destinos. Intenta de nuevo más tarde.'));
            } finally {
                setLoading(false);
            }
        };

        cargarDestinos();
    }, []);

    useEffect(() => {
        if (servicio !== 'restaurantes' || restaurantes !== null) return;
        fetchRestaurantes()
            .then(setRestaurantes)
            .catch((err: unknown) =>
                setErrorRestaurantes(obtenerMensajeError(err, 'No pudimos cargar los restaurantes. Intenta de nuevo más tarde.')),
            );
    }, [servicio, restaurantes]);

    const restaurantesFiltrados = useMemo(
        () =>
            (restaurantes ?? []).filter((r) =>
                !busqueda || `${r.nombre} ${r.ubicacion} ${r.tipoCocina}`.toLowerCase().includes(busqueda),
            ),
        [restaurantes, busqueda],
    );

    const resultados = useMemo(
        () =>
            busqueda
                ? destinos.filter((d) =>
                    `${d.nombre} ${d.ubicacion}`.toLowerCase().includes(busqueda),
                )
                : destinos,
        [destinos, busqueda],
    );

    const destacado = resultados[0];
    const ofertas = resultados.slice(1, 4);
    const populares = resultados.length > 4 ? resultados.slice(4, 6) : resultados.slice(0, 2);

    return (
        <div className="home">
            <div className="servicios" role="tablist" aria-label="Tipo de servicio">
                <button
                    type="button"
                    role="tab"
                    aria-selected={servicio === 'alojamiento'}
                    className={`servicios__tab ${servicio === 'alojamiento' ? 'servicios__tab--activo' : ''}`}
                    onClick={() => setServicio('alojamiento')}
                >
                    Alojamiento
                </button>
                <button
                    type="button"
                    role="tab"
                    aria-selected={servicio === 'restaurantes'}
                    className={`servicios__tab ${servicio === 'restaurantes' ? 'servicios__tab--activo' : ''}`}
                    onClick={() => setServicio('restaurantes')}
                >
                    Restaurantes
                </button>
            </div>

            {servicio === 'restaurantes' ? (
                <section className="seccion" role="tabpanel" aria-labelledby="titulo-restaurantes">
                    <h1 id="titulo-restaurantes" className="titulo-principal">Restaurantes ofertados</h1>
                    <p className="subtitulo">
                        {busqueda ? `Resultados para “${searchParams.get('destino')}”` : 'Lugares para comer bien durante tu viaje'}
                    </p>

                    {restaurantes === null && !errorRestaurantes && <p className="estado">Cargando restaurantes…</p>}
                    {errorRestaurantes && <p className="estado estado--error" role="alert">{errorRestaurantes}</p>}
                    {restaurantes !== null && restaurantesFiltrados.length === 0 && (
                        <p className="estado">No encontramos restaurantes para tu búsqueda. Prueba con otro nombre o ciudad.</p>
                    )}

                    <div className="rejilla rejilla--tres rejilla--restaurantes">
                        {restaurantesFiltrados.map((restaurante) => (
                            <TarjetaRestaurante key={restaurante.id} restaurante={restaurante} />
                        ))}
                    </div>
                </section>
            ) : (
                <>
                <section className="seccion" aria-labelledby="titulo-busqueda">
                    <h1 id="titulo-busqueda" className="titulo-principal">Busca tu próximo destino</h1>
                    <p className="subtitulo">
                        {busqueda ? `Resultados para “${searchParams.get('destino')}”` : 'Algunos lugares que te podrían gustar'}
                    </p>

                    {loading && <p className="estado">Cargando destinos…</p>}
                    {error && <p className="estado estado--error" role="alert">{error}</p>}
                    {!loading && !error && resultados.length === 0 && (
                        <p className="estado">No encontramos destinos para tu búsqueda. Prueba con otro nombre o ciudad.</p>
                    )}

                    {destacado && <HotelDestacado destino={destacado} />}
                </section>

                {ofertas.length > 0 && (
                    <section className="seccion" aria-labelledby="titulo-ofertas">
                        <h2 id="titulo-ofertas" className="titulo-seccion titulo-seccion--naranja">Ofertas</h2>
                        <div className="rejilla rejilla--tres">
                            {ofertas.map((destino) => (
                                <TarjetaDestino key={destino.id} destino={destino} variante="oferta" />
                            ))}
                        </div>
                    </section>
                )}

                {populares.length > 0 && (
                    <section className="seccion" id="destinos-populares" aria-labelledby="titulo-populares">
                        <h2 id="titulo-populares" className="titulo-seccion">Destinos populares</h2>
                        <div className="rejilla rejilla--dos">
                            {populares.map((destino) => (
                                <TarjetaDestino key={destino.id} destino={destino} variante="popular" />
                            ))}
                        </div>
                    </section>
                )}
                </>
            )}

            <div className="seccion">
                <PlanificadorViajes />
            </div>
        </div>
    );
};

export default Home;