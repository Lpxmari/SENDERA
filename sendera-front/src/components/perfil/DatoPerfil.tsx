import { ReactNode } from 'react';

const ICONOS: Record<string, string> = {
    identificacion: 'M3 6h18v12H3zM7 11h4M7 14h8',
    persona: 'M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM4 21a8 8 0 0 1 16 0',
    calendario: 'M4 6h16v14H4zM4 10h16M8 3v4M16 3v4',
    correo: 'M12 16a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM16 8v5a3 3 0 0 0 6 0v-1a10 10 0 1 0-4 8',
    ubicacion: 'M12 21s7-6.2 7-11a7 7 0 0 0-14 0c0 4.8 7 11 7 11zM12 12a2 2 0 1 0 0-4 2 2 0 0 0 0 4z',
    mapa: 'M3 6l6-2 6 2 6-2v14l-6 2-6-2-6 2zM9 4v14M15 6v14',
};

export type IconoDato = keyof typeof ICONOS;

interface DatoPerfilProps {
    icono: IconoDato;
    etiqueta: string;
    children: ReactNode;
}

/** Fila de solo lectura: ícono decorativo, etiqueta y valor. */
const DatoPerfil = ({ icono, etiqueta, children }: DatoPerfilProps) => (
    <div className="perfil-dato">
        <span className="perfil-dato__icono" aria-hidden="true">
            <svg
                viewBox="0 0 24 24"
                width="18"
                height="18"
                fill="none"
                stroke="currentColor"
                strokeWidth="1.8"
                strokeLinecap="round"
                strokeLinejoin="round"
            >
                <path d={ICONOS[icono]} />
            </svg>
        </span>
        <div className="perfil-dato__texto">
            <dt>{etiqueta}</dt>
            <dd>{children}</dd>
        </div>
    </div>
);

export default DatoPerfil;
