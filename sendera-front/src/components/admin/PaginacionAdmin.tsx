import BotonIcono from '../BotonIcono';

interface PaginacionAdminProps {
    etiqueta: string;
    pagina: number; // empieza en 0
    tamano: number;
    total: number;
    cargando: boolean;
    singular: string;
    plural: string;
    onCambiar: (pagina: number) => void;
}

/** Controles Anterior / Siguiente con el resumen "Página X de Y". */
const PaginacionAdmin = ({
    etiqueta,
    pagina,
    tamano,
    total,
    cargando,
    singular,
    plural,
    onCambiar,
}: PaginacionAdminProps) => {
    if (total === 0) return null;
    const totalPaginas = Math.max(1, Math.ceil(total / tamano));

    return (
        <nav className="admin-paginacion" aria-label={etiqueta}>
            <BotonIcono
                icono="izquierda"
                etiqueta="Anterior"
                ayuda="Página anterior"
                disabled={pagina === 0 || cargando}
                onClick={() => onCambiar(pagina - 1)}
            />
            <span>
                Página {pagina + 1} de {totalPaginas} · {total} {total === 1 ? singular : plural}
            </span>
            <BotonIcono
                icono="derecha"
                etiqueta="Siguiente"
                ayuda="Página siguiente"
                disabled={pagina + 1 >= totalPaginas || cargando}
                onClick={() => onCambiar(pagina + 1)}
            />
        </nav>
    );
};

export default PaginacionAdmin;
