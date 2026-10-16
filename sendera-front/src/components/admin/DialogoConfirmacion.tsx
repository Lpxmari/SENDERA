import { useEffect, useRef } from 'react';

interface DialogoConfirmacionProps {
    titulo: string;
    mensaje: string;
    textoConfirmar: string;
    procesando?: boolean;
    onConfirmar: () => void;
    onCancelar: () => void;
}

/** Confirmación antes de una acción que cambia datos. El foco inicia en "Cancelar" para evitar errores. */
const DialogoConfirmacion = ({
    titulo,
    mensaje,
    textoConfirmar,
    procesando = false,
    onConfirmar,
    onCancelar,
}: DialogoConfirmacionProps) => {
    const botonCancelar = useRef<HTMLButtonElement>(null);

    useEffect(() => {
        botonCancelar.current?.focus();
    }, []);

    useEffect(() => {
        const handleTecla = (evento: KeyboardEvent) => {
            if (evento.key === 'Escape' && !procesando) onCancelar();
        };
        document.addEventListener('keydown', handleTecla);
        return () => document.removeEventListener('keydown', handleTecla);
    }, [onCancelar, procesando]);

    return (
        <div className="admin-dialogo__fondo">
            <div
                className="admin-dialogo"
                role="alertdialog"
                aria-modal="true"
                aria-labelledby="admin-dialogo-titulo"
                aria-describedby="admin-dialogo-mensaje"
            >
                <h2 id="admin-dialogo-titulo">{titulo}</h2>
                <p id="admin-dialogo-mensaje">{mensaje}</p>
                <div className="admin-dialogo__acciones">
                    <button
                        type="button"
                        className="boton-contorno"
                        ref={botonCancelar}
                        onClick={onCancelar}
                        disabled={procesando}
                    >
                        Cancelar
                    </button>
                    <button type="button" className="boton boton--verde" onClick={onConfirmar} disabled={procesando}>
                        {procesando ? 'Guardando…' : textoConfirmar}
                    </button>
                </div>
            </div>
        </div>
    );
};

export default DialogoConfirmacion;
