import { ButtonHTMLAttributes } from 'react';
import IconoAdmin, { NombreIconoAdmin } from './admin/IconoAdmin';

interface BotonIconoProps extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'children' | 'title'> {
    icono: NombreIconoAdmin;
    /** Nombre accesible completo (qué hace y sobre qué), p. ej. "Eliminar la reseña de Ana". */
    etiqueta: string;
    /** Texto corto del tooltip; por defecto, la etiqueta. */
    ayuda?: string;
    peligro?: boolean;
    /** Ícono relleno (p. ej. un elemento guardado). */
    relleno?: boolean;
}

/** Botón cuadrado de solo ícono: el significado lo da el nombre accesible y el tooltip, nunca solo el dibujo. */
const BotonIcono = ({
    icono,
    etiqueta,
    ayuda,
    peligro = false,
    relleno = false,
    className = '',
    ...resto
}: BotonIconoProps) => (
    <button
        type="button"
        className={`boton-icono ${peligro ? 'boton-icono--peligro' : ''} ${className}`.trim()}
        aria-label={etiqueta}
        title={ayuda ?? etiqueta}
        {...resto}
    >
        <IconoAdmin nombre={icono} relleno={relleno} />
    </button>
);

export default BotonIcono;
