interface PaginacionResultadosProps {
    /** Página actual, desde 0. */
    pagina: number;
    totalPaginas: number;
    cargando?: boolean;
    onCambiar: (pagina: number) => void;
}

/** «Anterior» / «Siguiente» con texto visible y «Página X de Y»; los extremos quedan deshabilitados. */
const PaginacionResultados = ({ pagina, totalPaginas, cargando = false, onCambiar }: PaginacionResultadosProps) => {
    if (totalPaginas <= 1) return null;
    const actual = pagina + 1;

    return (
        <nav className="paginacion" aria-label="Paginación de resultados">
            <button
                type="button"
                className="boton boton--verde paginacion__boton"
                disabled={pagina <= 0 || cargando}
                onClick={() => onCambiar(pagina - 1)}
                aria-label={`Ir a la página anterior, la ${actual - 1} de ${totalPaginas}`}
            >
                Anterior
            </button>
            <span className="paginacion__estado" aria-live="polite">
                Página {actual} de {totalPaginas}
            </span>
            <button
                type="button"
                className="boton boton--verde paginacion__boton"
                disabled={actual >= totalPaginas || cargando}
                onClick={() => onCambiar(pagina + 1)}
                aria-label={`Ir a la página siguiente, la ${actual + 1} de ${totalPaginas}`}
            >
                Siguiente
            </button>
        </nav>
    );
};

export default PaginacionResultados;
