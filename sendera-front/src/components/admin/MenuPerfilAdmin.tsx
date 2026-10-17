import { useEffect, useRef, useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { iniciales } from '../../utils/iniciales';

/**
 * Menú "Perfil" de la barra de administración: al pulsar la foto muestra solo el nombre y el correo.
 * No enlaza a la página de perfil de los viajeros, que un administrador no necesita.
 */
const MenuPerfilAdmin = () => {
    const { usuario } = useAuth();
    const [abierto, setAbierto] = useState(false);
    const contenedor = useRef<HTMLDivElement>(null);

    useEffect(() => {
        if (!abierto) return;
        const handleClicFuera = (evento: MouseEvent) => {
            if (contenedor.current && !contenedor.current.contains(evento.target as Node)) setAbierto(false);
        };
        const handleTecla = (evento: KeyboardEvent) => {
            if (evento.key === 'Escape') setAbierto(false);
        };
        document.addEventListener('mousedown', handleClicFuera);
        document.addEventListener('keydown', handleTecla);
        return () => {
            document.removeEventListener('mousedown', handleClicFuera);
            document.removeEventListener('keydown', handleTecla);
        };
    }, [abierto]);

    if (!usuario) return null;
    const nombreCompleto = `${usuario.nombre} ${usuario.apellidos}`;

    return (
        <div className="admin-perfil" ref={contenedor}>
            <button
                type="button"
                className="admin-perfil__boton"
                aria-expanded={abierto}
                aria-controls="admin-perfil-panel"
                aria-label={`Perfil de ${usuario.nombre}`}
                onClick={() => setAbierto((actual) => !actual)}
            >
                <span className="admin-avatar" aria-hidden="true">
                    {iniciales(nombreCompleto)}
                </span>
                <span>{usuario.nombre}</span>
                <svg
                    viewBox="0 0 24 24"
                    width="16"
                    height="16"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    aria-hidden="true"
                    focusable="false"
                >
                    <path d="m6 9 6 6 6-6" />
                </svg>
            </button>

            {abierto && (
                <div id="admin-perfil-panel" className="admin-perfil__panel" role="region" aria-label="Perfil">
                    <p className="admin-perfil__titulo">Perfil</p>
                    <div className="admin-perfil__datos">
                        <span className="admin-avatar" aria-hidden="true">
                            {iniciales(nombreCompleto)}
                        </span>
                        <div>
                            <strong>{nombreCompleto}</strong>
                            <span>{usuario.correo}</span>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default MenuPerfilAdmin;
