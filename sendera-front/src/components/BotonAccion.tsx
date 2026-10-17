import { ButtonHTMLAttributes } from 'react';
import IconoAdmin, { NombreIconoAdmin } from './admin/IconoAdmin';

interface BotonAccionProps extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'children'> {
    icono: NombreIconoAdmin;
    /** Texto visible, corto (p. ej. "Confirmar"). */
    texto: string;
    /** Nombre accesible completo; debe contener el texto visible (p. ej. "Confirmar la reserva 3 de Ana"). */
    etiqueta: string;
    peligro?: boolean;
}

/** Acción de tabla con ícono y texto, todas del mismo ancho para que las columnas queden alineadas. */
const BotonAccion = ({ icono, texto, etiqueta, peligro = false, className = '', ...resto }: BotonAccionProps) => (
    <button
        type="button"
        className={`boton-accion ${peligro ? 'boton-accion--peligro' : ''} ${className}`.trim()}
        aria-label={etiqueta}
        {...resto}
    >
        <IconoAdmin nombre={icono} />
        <span>{texto}</span>
    </button>
);

export default BotonAccion;
