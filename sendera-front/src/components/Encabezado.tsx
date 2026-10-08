import { FormEvent, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import logoSendera from '../assets/logo-sendera.png';

const Encabezado = () => {
    const navigate = useNavigate();
    const [destino, setDestino] = useState<string>('');
    const [llegada, setLlegada] = useState<string>('');
    const [salida, setSalida] = useState<string>('');
    const [viajeros, setViajeros] = useState<string>('2');

    const handleSubmit = (evento: FormEvent<HTMLFormElement>) => {
        evento.preventDefault();
        const params = new URLSearchParams();
        if (destino.trim()) params.set('destino', destino.trim());
        if (llegada) params.set('llegada', llegada);
        if (salida) params.set('salida', salida);
        params.set('viajeros', viajeros);
        navigate({ pathname: '/', search: params.toString() });
    };

    return (
        <header className="encabezado">
            <Link to="/" className="encabezado__logo" aria-label="Sendera, inicio">
                <img src={logoSendera} alt="Sendera agente de turismo" />
            </Link>

            <form className="buscador" role="search" onSubmit={handleSubmit}>
                <label className="buscador_campo buscador_campo--destino">
                    <span>Destino</span>
                    <input
                        type="text"
                        placeholder="¿A dónde vas?"
                        value={destino}
                        onChange={(e) => setDestino(e.target.value)}
                    />
                </label>
                <label className="buscador__campo">
                    <span>Llegada</span>
                    <input type="date" value={llegada} onChange={(e) => setLlegada(e.target.value)} />
                </label>
                <label className="buscador__campo">
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
                    <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" aria-hidden="true">
                        <circle cx="11" cy="11" r="7" />
                        <path d="m20 20-3.5-3.5" />
                    </svg>
                </button>
            </form>

            <nav className="encabezado__nav" aria-label="Principal">
                <a href="#destinos-populares">Destinos</a>
                <Link to="/">Inicio</Link>
                <button type="button" className="boton boton--verde">Iniciar Sesión</button>
            </nav>
        </header>
    );
};

export default Encabezado;