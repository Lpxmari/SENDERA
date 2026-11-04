import { FormEvent, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import logoSendera from '../assets/logo-sendera.png';
import { useAuth } from '../context/AuthContext';
import IconoAdmin from './admin/IconoAdmin';
import MenuCuenta from './MenuCuenta';

interface EncabezadoProps {
    /**
     * `completo` (con buscador) en el resto del sitio; `itinerarios` (sin buscador, con "Mi Perfil") en la lista y el
     * formulario de itinerarios; `detalle` (solo el botón "Mi Perfil") en la vista de un itinerario.
     */
    variante?: 'completo' | 'itinerarios' | 'detalle';
}

const Encabezado = ({ variante = 'completo' }: EncabezadoProps) => {
    const navigate = useNavigate();
    const { pathname } = useLocation();
    // El enlace "Destinos" lleva a una sección del inicio: solo tiene sentido en esa pantalla.
    const enInicio = pathname === '/';
    const { usuario, autenticado, esAdmin, logout } = useAuth();
    const [destino, setDestino] = useState<string>('');
    const [llegada, setLlegada] = useState<string>('');
    const [salida, setSalida] = useState<string>('');
    const [viajeros, setViajeros] = useState<string>('2');

    const handleCerrarSesion = () => {
        logout();
        navigate('/');
    };

    const handleSubmit = (evento: FormEvent<HTMLFormElement>) => {
        evento.preventDefault();
        const params = new URLSearchParams();
        if (destino.trim()) params.set('destino', destino.trim());
        if (llegada) params.set('llegada', llegada);
        if (salida) params.set('salida', salida);
        params.set('viajeros', viajeros);
        navigate({ pathname: '/resultados', search: params.toString() });
    };

    return (
        <header className={variante === 'completo' ? 'encabezado' : 'encabezado encabezado--sin-buscador'}>
            <Link to="/" className="encabezado__logo" aria-label="Sendera, inicio">
                <img src={logoSendera} alt="Sendera agente de turismo" />
            </Link>

            {variante === 'completo' && (
                <form className="buscador" role="search" onSubmit={handleSubmit}>
                    <label className="buscador__campo buscador__campo--destino">
                        <span>Destino</span>
                        <input
                            type="text"
                            placeholder="¿A dónde vas?"
                            value={destino}
                            onChange={(e) => setDestino(e.target.value)}
                        />
                    </label>
                    <label className="buscador__campo buscador__campo--fecha">
                        <span>Llegada</span>
                        <input type="date" value={llegada} onChange={(e) => setLlegada(e.target.value)} />
                    </label>
                    <label className="buscador__campo buscador__campo--fecha">
                        <span>Salida</span>
                        <input type="date" value={salida} min={llegada} onChange={(e) => setSalida(e.target.value)} />
                    </label>
                    <label className="buscador__campo">
                        <span>Viajeros</span>
                        <select value={viajeros} onChange={(e) => setViajeros(e.target.value)}>
                            {[1, 2, 3, 4, 5, 6].map((n) => (
                                <option key={n} value={n}>
                                    {n} {n === 1 ? 'viajero' : 'viajeros'}
                                </option>
                            ))}
                        </select>
                    </label>
                    <button type="submit" className="buscador__boton" aria-label="Buscar destinos">
                        <svg
                            viewBox="0 0 24 24"
                            width="18"
                            height="18"
                            fill="none"
                            stroke="currentColor"
                            strokeWidth="2.4"
                            strokeLinecap="round"
                            aria-hidden="true"
                        >
                            <circle cx="11" cy="11" r="7" />
                            <path d="m20 20-3.5-3.5" />
                        </svg>
                    </button>
                </form>
            )}

            {variante === 'detalle' ? (
                <nav className="encabezado__nav" aria-label="Principal">
                    <Link to="/perfil" className="encabezado__volver">
                        <IconoAdmin nombre="izquierda" />
                        Mi Perfil
                    </Link>
                </nav>
            ) : (
                <nav className="encabezado__nav" aria-label="Principal">
                    {variante === 'itinerarios' && <Link to="/perfil">Mi Perfil</Link>}
                    {variante === 'itinerarios' && <a href="/#destinos-populares">Destinos</a>}
                    {variante === 'completo' && enInicio && <a href="#destinos-populares">Destinos</a>}
                    <Link to="/">Inicio</Link>
                    {autenticado ? (
                        <>
                            {usuario && (
                                <MenuCuenta
                                    usuario={usuario}
                                    onCerrarSesion={handleCerrarSesion}
                                    acceso={
                                        esAdmin
                                            ? { ruta: '/admin', texto: 'Administración' }
                                            : { ruta: '/perfil', texto: 'Ver mi perfil' }
                                    }
                                />
                            )}
                        </>
                    ) : (
                        <Link to="/login" className="boton boton--verde">
                            Iniciar Sesión
                        </Link>
                    )}
                </nav>
            )}
        </header>
    );
};

export default Encabezado;
