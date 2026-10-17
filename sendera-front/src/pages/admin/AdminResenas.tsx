import { FormEvent, useCallback, useState } from 'react';
import BotonIcono from '../../components/BotonIcono';
import DialogoConfirmacion from '../../components/admin/DialogoConfirmacion';
import IconoAdmin from '../../components/admin/IconoAdmin';
import PaginacionAdmin from '../../components/admin/PaginacionAdmin';
import { useConsultaAdmin } from '../../components/admin/useConsultaAdmin';
import { eliminarResenaAdmin, listarResenasAdmin } from '../../services/adminService';
import { obtenerMensajeError } from '../../services/apiClient';
import { listarDestinos } from '../../services/destinoService';
import { FiltroResenasAdmin, ResenaAdmin } from '../../types/admin';
import { iniciales } from '../../utils/iniciales';

const TAMANO_PAGINA = 10;
const MENSAJE_CARGA = 'No pudimos cargar las reseñas. Intenta de nuevo.';
const MENSAJE_DESTINOS = 'No pudimos cargar los destinos para el filtro.';

const MESES = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'];

/** ISO a "16 Oct 2026", sin depender del idioma del navegador. */
const formatearFecha = (iso: string): string => {
    const [anio, mes, dia] = iso.slice(0, 10).split('-').map(Number);
    return `${dia} ${MESES[mes - 1]} ${anio}`;
};

const AdminResenas = () => {
    const [filtros, setFiltros] = useState<FiltroResenasAdmin>({ destinoId: '', calificacionMax: '', q: '' });
    const [aplicados, setAplicados] = useState<FiltroResenasAdmin>({});
    const [pagina, setPagina] = useState(0);
    const [recarga, setRecarga] = useState(0);
    const [aviso, setAviso] = useState<string | null>(null);
    const [errorAccion, setErrorAccion] = useState<string | null>(null);
    const [porEliminar, setPorEliminar] = useState<ResenaAdmin | null>(null);
    const [eliminando, setEliminando] = useState(false);

    const clave = JSON.stringify([aplicados, pagina, recarga]);
    const { datos, cargando, error } = useConsultaAdmin(
        clave,
        () => listarResenasAdmin({ ...aplicados, pagina, tamano: TAMANO_PAGINA }),
        MENSAJE_CARGA,
    );
    const { datos: destinos } = useConsultaAdmin('destinos', () => listarDestinos(), MENSAJE_DESTINOS);

    const handleBuscar = (evento: FormEvent<HTMLFormElement>) => {
        evento.preventDefault();
        setAviso(null);
        setErrorAccion(null);
        setPagina(0);
        setAplicados({ ...filtros });
    };

    const handleLimpiar = () => {
        setFiltros({ destinoId: '', calificacionMax: '', q: '' });
        setAviso(null);
        setErrorAccion(null);
        setPagina(0);
        setAplicados({});
    };

    const cancelar = useCallback(() => setPorEliminar(null), []);

    const confirmarEliminar = async () => {
        if (!porEliminar) return;
        setEliminando(true);
        try {
            await eliminarResenaAdmin(porEliminar.id);
            setAviso(`Se eliminó la reseña de ${porEliminar.autor}.`);
            setErrorAccion(null);
            // Si era la única de la última página, se retrocede una para no quedar en una página vacía.
            if (datos && datos.contenido.length === 1 && pagina > 0) setPagina(pagina - 1);
            setRecarga((n) => n + 1);
        } catch (err) {
            setAviso(null);
            setErrorAccion(obtenerMensajeError(err, 'No pudimos eliminar la reseña. Intenta de nuevo.'));
            setRecarga((n) => n + 1);
        } finally {
            setEliminando(false);
            setPorEliminar(null);
        }
    };

    const mensajeError = errorAccion ?? error;

    return (
        <section aria-labelledby="admin-resenas-titulo">
            <h1 id="admin-resenas-titulo" className="admin__h1">
                Reseñas
            </h1>
            <p className="admin__ayuda">
                Lee las opiniones de los viajeros y elimina las que no sean apropiadas. La eliminación queda registrada
                en la auditoría.
            </p>

            <form className="admin-filtros" onSubmit={handleBuscar} role="search" aria-label="Buscar reseñas">
                <label className="admin-campo admin-campo--ancho" htmlFor="resenas-q">
                    <span>Comentario o autor</span>
                    <input
                        id="resenas-q"
                        type="search"
                        value={filtros.q ?? ''}
                        onChange={(e) => setFiltros({ ...filtros, q: e.target.value })}
                    />
                </label>
                <label className="admin-campo" htmlFor="resenas-destino">
                    <span>Destino</span>
                    <select
                        id="resenas-destino"
                        value={
                            filtros.destinoId === '' || filtros.destinoId === undefined ? '' : String(filtros.destinoId)
                        }
                        onChange={(e) =>
                            setFiltros({ ...filtros, destinoId: e.target.value === '' ? '' : Number(e.target.value) })
                        }
                    >
                        <option value="">Todos</option>
                        {(destinos ?? []).map((d) => (
                            <option key={d.id} value={d.id}>
                                {d.nombre}
                            </option>
                        ))}
                    </select>
                </label>
                <label className="admin-campo" htmlFor="resenas-calificacion">
                    <span>Calificación</span>
                    <select
                        id="resenas-calificacion"
                        value={
                            filtros.calificacionMax === '' || filtros.calificacionMax === undefined
                                ? ''
                                : String(filtros.calificacionMax)
                        }
                        onChange={(e) =>
                            setFiltros({
                                ...filtros,
                                calificacionMax: e.target.value === '' ? '' : Number(e.target.value),
                            })
                        }
                    >
                        <option value="">Todas</option>
                        <option value="1">Solo 1 estrella</option>
                        <option value="2">Hasta 2 estrellas</option>
                        <option value="3">Hasta 3 estrellas</option>
                        <option value="4">Hasta 4 estrellas</option>
                    </select>
                </label>
                <div className="admin-filtros__acciones">
                    <button type="submit" className="boton boton--verde boton--con-icono">
                        <IconoAdmin nombre="lupa" />
                        Buscar
                    </button>
                    <button type="button" className="boton-contorno boton--con-icono" onClick={handleLimpiar}>
                        <IconoAdmin nombre="limpiar" />
                        Limpiar
                    </button>
                </div>
            </form>

            {aviso && (
                <p className="admin-aviso" role="status">
                    {aviso}
                </p>
            )}
            {mensajeError && (
                <p className="admin-error" role="alert">
                    {mensajeError}
                </p>
            )}

            {cargando && !datos && <p role="status">Cargando reseñas…</p>}
            {datos && datos.contenido.length === 0 && !cargando && (
                <p className="admin-vacio">No encontramos reseñas con esos filtros.</p>
            )}

            {datos && datos.contenido.length > 0 && (
                <ul className="admin-resenas" aria-label="Lista de reseñas" aria-busy={cargando}>
                    {datos.contenido.map((r) => (
                        <li key={r.id} className={`admin-resena ${r.calificacion <= 2 ? 'admin-resena--baja' : ''}`}>
                            <div className="admin-resena__cabecera">
                                <span className="admin-avatar" aria-hidden="true">
                                    {iniciales(r.autor)}
                                </span>
                                <div className="admin-resena__autor">
                                    <strong>{r.autor}</strong>
                                    <span>{formatearFecha(r.creadaEn)}</span>
                                </div>
                                <span aria-hidden="true" className="admin-estrellas">
                                    {'★'.repeat(r.calificacion)}
                                    {'☆'.repeat(5 - r.calificacion)}
                                </span>
                                <span className="sr-only">{r.calificacion} de 5 estrellas</span>
                            </div>

                            <span className="admin-resena__destino">{r.destinoNombre}</span>
                            <p className="admin-resena__comentario">{r.comentario}</p>

                            <div className="admin-resena__pie">
                                {r.tieneImagen ? (
                                    <a
                                        className="boton-icono"
                                        href={`/api/v1/destinos/${r.destinoId}/resenas/${r.id}/imagen`}
                                        target="_blank"
                                        rel="noreferrer"
                                        aria-label={`Ver la imagen de la reseña de ${r.autor}`}
                                        title="Ver imagen adjunta"
                                    >
                                        <IconoAdmin nombre="imagen" />
                                    </a>
                                ) : (
                                    <span className="admin-etiqueta">Sin imagen</span>
                                )}
                                <BotonIcono
                                    icono="papelera"
                                    etiqueta={`Eliminar la reseña de ${r.autor} sobre ${r.destinoNombre}`}
                                    ayuda="Eliminar reseña"
                                    peligro
                                    onClick={() => setPorEliminar(r)}
                                />
                            </div>
                        </li>
                    ))}
                </ul>
            )}

            {datos && (
                <PaginacionAdmin
                    etiqueta="Paginación de reseñas"
                    pagina={pagina}
                    tamano={datos.tamano}
                    total={datos.total}
                    cargando={cargando}
                    singular="reseña"
                    plural="reseñas"
                    onCambiar={setPagina}
                />
            )}

            {porEliminar && (
                <DialogoConfirmacion
                    peligro
                    titulo="Eliminar reseña"
                    mensaje={`¿Seguro que quieres eliminar la reseña de ${porEliminar.autor} sobre “${porEliminar.destinoNombre}”? Esta acción no se puede deshacer.`}
                    textoConfirmar="Sí, eliminar"
                    procesando={eliminando}
                    onConfirmar={confirmarEliminar}
                    onCancelar={cancelar}
                />
            )}
        </section>
    );
};

export default AdminResenas;
