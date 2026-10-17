import { FormEvent, useCallback, useState } from 'react';
import BotonAccion from '../../components/BotonAccion';
import DialogoConfirmacion from '../../components/admin/DialogoConfirmacion';
import IconoAdmin from '../../components/admin/IconoAdmin';
import PaginacionAdmin from '../../components/admin/PaginacionAdmin';
import { useConsultaAdmin } from '../../components/admin/useConsultaAdmin';
import { cambiarEstadoReserva, listarReservasAdmin } from '../../services/adminService';
import { obtenerMensajeError } from '../../services/apiClient';
import { FiltroReservasAdmin, ReservaAdmin } from '../../types/admin';
import { EstadoReserva } from '../../types/reserva';
import { formatearPrecio } from '../../utils/formatearPrecio';

const TAMANO_PAGINA = 10;
const MENSAJE_CARGA = 'No pudimos cargar las reservas. Intenta de nuevo.';

const ETIQUETA_ESTADO: Record<EstadoReserva, string> = {
    PENDIENTE: 'Pendiente',
    CONFIRMADA: 'Confirmada',
    CANCELADA: 'Cancelada',
};

const MESES = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'];

/** YYYY-MM-DD a "16 Oct 2026", sin depender del idioma del navegador. */
const formatearFecha = (iso: string): string => {
    const [anio, mes, dia] = iso.slice(0, 10).split('-').map(Number);
    return `${dia} ${MESES[mes - 1]} ${anio}`;
};

type Accion = { reserva: ReservaAdmin; estado: 'CONFIRMADA' | 'CANCELADA' };

const describirAccion = ({ reserva, estado }: Accion) => {
    const detalle = `la reserva n.º ${reserva.id} de ${reserva.usuarioNombre} para “${reserva.itinerarioNombre}”`;
    return estado === 'CONFIRMADA'
        ? { titulo: 'Confirmar reserva', mensaje: `¿Quieres confirmar ${detalle}?`, confirmar: 'Sí, confirmar' }
        : {
              titulo: 'Cancelar reserva',
              mensaje: `¿Quieres cancelar ${detalle}? Se devolverá el cupo al itinerario y la reserva no podrá reactivarse.`,
              confirmar: 'Sí, cancelar reserva',
          };
};

const AdminReservas = () => {
    const [filtros, setFiltros] = useState<FiltroReservasAdmin>({ estado: '', q: '', desde: '', hasta: '' });
    const [aplicados, setAplicados] = useState<FiltroReservasAdmin>({});
    const [pagina, setPagina] = useState(0);
    const [recarga, setRecarga] = useState(0);
    const [errorFiltros, setErrorFiltros] = useState<string | null>(null);
    const [aviso, setAviso] = useState<string | null>(null);
    const [errorAccion, setErrorAccion] = useState<string | null>(null);
    const [pendiente, setPendiente] = useState<Accion | null>(null);
    const [procesando, setProcesando] = useState(false);

    const clave = JSON.stringify([aplicados, pagina, recarga]);
    const { datos, cargando, error } = useConsultaAdmin(
        clave,
        () => listarReservasAdmin({ ...aplicados, pagina, tamano: TAMANO_PAGINA }),
        MENSAJE_CARGA,
    );

    const handleBuscar = (evento: FormEvent<HTMLFormElement>) => {
        evento.preventDefault();
        if (filtros.desde && filtros.hasta && filtros.desde > filtros.hasta) {
            setErrorFiltros('La fecha inicial no puede ser posterior a la final.');
            return;
        }
        setErrorFiltros(null);
        setAviso(null);
        setErrorAccion(null);
        setPagina(0);
        setAplicados({ ...filtros });
    };

    const handleLimpiar = () => {
        setFiltros({ estado: '', q: '', desde: '', hasta: '' });
        setErrorFiltros(null);
        setAviso(null);
        setErrorAccion(null);
        setPagina(0);
        setAplicados({});
    };

    const cancelar = useCallback(() => setPendiente(null), []);

    const confirmar = async () => {
        if (!pendiente) return;
        setProcesando(true);
        try {
            await cambiarEstadoReserva(pendiente.reserva.id, pendiente.estado);
            setAviso(
                pendiente.estado === 'CONFIRMADA'
                    ? `La reserva n.º ${pendiente.reserva.id} quedó confirmada.`
                    : `La reserva n.º ${pendiente.reserva.id} quedó cancelada y se devolvió el cupo.`,
            );
            setErrorAccion(null);
            setRecarga((n) => n + 1);
        } catch (err) {
            setAviso(null);
            setErrorAccion(obtenerMensajeError(err, 'No pudimos cambiar el estado de la reserva. Intenta de nuevo.'));
            // La reserva pudo cambiar por otra persona: se vuelve a consultar para mostrar el estado real.
            setRecarga((n) => n + 1);
        } finally {
            setProcesando(false);
            setPendiente(null);
        }
    };

    const mensajeError = errorAccion ?? error;

    return (
        <section aria-labelledby="admin-reservas-titulo">
            <h1 id="admin-reservas-titulo" className="admin__h1">
                Reservas
            </h1>
            <p className="admin__ayuda">
                Consulta las reservas de todos los viajeros, confírmalas o cancélalas. Los cambios quedan registrados en
                la auditoría.
            </p>

            <form className="admin-filtros" onSubmit={handleBuscar} role="search" aria-label="Buscar reservas">
                <label className="admin-campo admin-campo--ancho" htmlFor="reservas-q">
                    <span>Viajero, correo o destino</span>
                    <input
                        id="reservas-q"
                        type="search"
                        value={filtros.q ?? ''}
                        onChange={(e) => setFiltros({ ...filtros, q: e.target.value })}
                    />
                </label>
                <label className="admin-campo" htmlFor="reservas-estado">
                    <span>Estado</span>
                    <select
                        id="reservas-estado"
                        value={filtros.estado ?? ''}
                        onChange={(e) => setFiltros({ ...filtros, estado: e.target.value as EstadoReserva | '' })}
                    >
                        <option value="">Todos</option>
                        <option value="PENDIENTE">Pendientes</option>
                        <option value="CONFIRMADA">Confirmadas</option>
                        <option value="CANCELADA">Canceladas</option>
                    </select>
                </label>
                <label className="admin-campo" htmlFor="reservas-desde">
                    <span>Reservadas desde</span>
                    <input
                        id="reservas-desde"
                        type="date"
                        value={filtros.desde ?? ''}
                        onChange={(e) => setFiltros({ ...filtros, desde: e.target.value })}
                    />
                </label>
                <label className="admin-campo" htmlFor="reservas-hasta">
                    <span>Reservadas hasta</span>
                    <input
                        id="reservas-hasta"
                        type="date"
                        value={filtros.hasta ?? ''}
                        min={filtros.desde || undefined}
                        onChange={(e) => setFiltros({ ...filtros, hasta: e.target.value })}
                    />
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

            {errorFiltros && (
                <p className="admin-error" role="alert">
                    {errorFiltros}
                </p>
            )}
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

            {cargando && !datos && <p role="status">Cargando reservas…</p>}
            {datos && datos.contenido.length === 0 && !cargando && (
                <p className="admin-vacio">No encontramos reservas con esos filtros.</p>
            )}

            {datos && datos.contenido.length > 0 && (
                <div className="admin-tabla__contenedor" aria-busy={cargando}>
                    <table className="admin-tabla">
                        <caption className="sr-only">Lista de reservas</caption>
                        <thead>
                            <tr>
                                <th scope="col">N.º</th>
                                <th scope="col">Viajero</th>
                                <th scope="col">Destino e itinerario</th>
                                <th scope="col">Fecha de reserva</th>
                                <th scope="col">Monto</th>
                                <th scope="col">Estado</th>
                                <th scope="col">Acciones</th>
                            </tr>
                        </thead>
                        <tbody>
                            {datos.contenido.map((r) => (
                                <tr key={r.id}>
                                    <td>{r.id}</td>
                                    <td>
                                        {r.usuarioNombre}
                                        <br />
                                        <span className="admin-etiqueta">{r.usuarioCorreo}</span>
                                    </td>
                                    <td>
                                        {r.destinoNombre}
                                        <br />
                                        <span className="admin-etiqueta">{r.itinerarioNombre}</span>
                                    </td>
                                    <td>{formatearFecha(r.fechaReserva)}</td>
                                    <td>{formatearPrecio(r.montoTotal)}</td>
                                    <td>
                                        <span className={`admin-estado admin-estado--${r.estadoReserva.toLowerCase()}`}>
                                            {ETIQUETA_ESTADO[r.estadoReserva]}
                                        </span>
                                    </td>
                                    <td>
                                        {r.estadoReserva === 'CANCELADA' ? (
                                            <span className="admin-etiqueta">Sin acciones</span>
                                        ) : (
                                            <div className="admin-tabla__acciones">
                                                {r.estadoReserva === 'PENDIENTE' && (
                                                    <BotonAccion
                                                        icono="check"
                                                        texto="Confirmar"
                                                        etiqueta={`Confirmar la reserva ${r.id} de ${r.usuarioNombre}`}
                                                        onClick={() =>
                                                            setPendiente({ reserva: r, estado: 'CONFIRMADA' })
                                                        }
                                                    />
                                                )}
                                                <BotonAccion
                                                    icono="equis"
                                                    texto="Cancelar"
                                                    etiqueta={`Cancelar la reserva ${r.id} de ${r.usuarioNombre}`}
                                                    peligro
                                                    onClick={() => setPendiente({ reserva: r, estado: 'CANCELADA' })}
                                                />
                                            </div>
                                        )}
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}

            {datos && (
                <PaginacionAdmin
                    etiqueta="Paginación de reservas"
                    pagina={pagina}
                    tamano={datos.tamano}
                    total={datos.total}
                    cargando={cargando}
                    singular="reserva"
                    plural="reservas"
                    onCambiar={setPagina}
                />
            )}

            {pendiente && (
                <DialogoConfirmacion
                    peligro={pendiente.estado === 'CANCELADA'}
                    titulo={describirAccion(pendiente).titulo}
                    mensaje={describirAccion(pendiente).mensaje}
                    textoConfirmar={describirAccion(pendiente).confirmar}
                    procesando={procesando}
                    onConfirmar={confirmar}
                    onCancelar={cancelar}
                />
            )}
        </section>
    );
};

export default AdminReservas;
