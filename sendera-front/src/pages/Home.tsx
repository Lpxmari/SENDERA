import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { fetchDestinos } from '../services/destinoService';
import { Destino } from '../types/destino';
import TarjetaDestino from '../components/TarjetaDestino';
import HotelDestacado from '../components/HotelDestacado';
import PlanificadorViajes from '../components/PlanificadorViajes';

type TipoServicio = 'alojamiento' | 'restaurantes';

const Home = () => {
    const [destinos, setDestinos] = useState<Destino[]>([]);
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);
    const [servicio, setServicio] = useState<TipoServicio>('alojamiento');
    const [searchParams] = useSearchParams();
    const busqueda = (searchParams.get('destino') ?? '').trim().toLowerCase();

    useEffect(() => {
        const cargarDestinos = async () => {
            try {
                const data = await fetchDestinos();
                setDestinos(data);
            } catch (err) {
                setError('No pudimos cargar los destinos. Verifica que el backend esté corriendo en el puerto 8080.');
            } finally {
                setLoading(false);
            }
        };

        cargarDestinos();
    }, []);

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

            <div className="seccion">
                <PlanificadorViajes />
            </div>
        </div>
    );
};

export default Home;