import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { Usuario } from '../types/auth';
import IconoAdmin from './admin/IconoAdmin';
import { iniciales } from '../utils/iniciales';

interface MenuCuentaProps {
    usuario: Usuario;
    onCerrarSesion: () => void;
    /** Opción principal del menú: "Ver mi perfil" para viajeros, "Administración" para administradores. */
    acceso: { ruta: string; texto: string };
}

/** Menú del viajero en el encabezado: foto (iniciales) y nombre; al pulsar despliega su acceso principal y "Cerrar sesión". */
const MenuCuenta = ({ usuario, onCerrarSesion, acceso }: MenuCuentaProps) => {
    const [abierto, setAbierto] = useState(false);
    const contenedor = useRef<HTMLDivElement>(null);
    const nombreCompleto = `${usuario.nombre} ${usuario.apellidos}`;

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

    return (
        <div className="menu-cuenta" ref={contenedor}>
            <button
                type="button"
                className="menu-cuenta__boton"
                aria-expanded={abierto}
                aria-controls="menu-cuenta-panel"
                aria-label={`Mi cuenta, ${usuario.nombre}`}
                onClick={() => setAbierto((actual) => !actual)}
            >
                <span className="menu-cuenta__avatar" aria-hidden="true">
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
                <div id="menu-cuenta-panel" className="menu-cuenta__panel">
                    <p className="menu-cuenta__correo">{usuario.correo}</p>
                    <Link to={acceso.ruta} className="menu-cuenta__enlace" onClick={() => setAbierto(false)}>
                        {acceso.texto}
                    </Link>
                    <button
                        type="button"
                        className="menu-cuenta__salir"
                        onClick={() => {
                            setAbierto(false);
                            onCerrarSesion();
                        }}
                    >
                        <IconoAdmin nombre="salir" />
                        Cerrar sesión
                    </button>
                </div>
            )}
        </div>
    );
};

export default MenuCuenta;
