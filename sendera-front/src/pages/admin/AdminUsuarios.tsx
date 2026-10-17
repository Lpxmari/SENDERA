import { FormEvent, useCallback, useEffect, useState } from 'react';
import BotonAccion from '../../components/BotonAccion';
import DialogoConfirmacion from '../../components/admin/DialogoConfirmacion';
import PaginacionAdmin from '../../components/admin/PaginacionAdmin';
import IconoAdmin from '../../components/admin/IconoAdmin';
import { useAuth } from '../../context/AuthContext';
import { cambiarActivoUsuario, cambiarRolUsuario, listarUsuariosAdmin } from '../../services/adminService';
import { obtenerMensajeError } from '../../services/apiClient';
import { FiltroUsuariosAdmin, PaginaUsuarios, RolUsuario, UsuarioAdmin } from '../../types/admin';
import { iniciales } from '../../utils/iniciales';

const TAMANO_PAGINA = 10;

type Accion =
    | { tipo: 'rol'; usuario: UsuarioAdmin; rol: RolUsuario }
    | { tipo: 'activo'; usuario: UsuarioAdmin; activo: boolean };

const ETIQUETA_ROL: Record<RolUsuario, string> = { ROLE_ADMIN: 'Administrador', ROLE_CLIENTE: 'Cliente' };

const MESES = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'];

/** ISO a "16 Oct 2026", sin depender del idioma del navegador. */
const formatearFecha = (iso: string): string => {
    const [anio, mes, dia] = iso.slice(0, 10).split('-').map(Number);
    return `${dia} ${MESES[mes - 1]} ${anio}`;
};

const nombreCompleto = (u: UsuarioAdmin): string => `${u.nombre} ${u.apellidos}`;

const describirAccion = (accion: Accion): { titulo: string; mensaje: string; confirmar: string } => {
    const nombre = nombreCompleto(accion.usuario);
    if (accion.tipo === 'rol') {
        return accion.rol === 'ROLE_ADMIN'
            ? {
                  titulo: 'Dar permisos de administrador',
                  mensaje: `¿Quieres que ${nombre} sea administrador? Podrá gestionar usuarios, destinos y reservas.`,
                  confirmar: 'Sí, hacer administrador',
              }
            : {
                  titulo: 'Quitar permisos de administrador',
                  mensaje: `¿Quieres que ${nombre} vuelva a ser cliente? Perderá el acceso a la administración.`,
                  confirmar: 'Sí, hacer cliente',
              };
    }
    return accion.activo
        ? {
              titulo: 'Activar cuenta',
              mensaje: `¿Quieres devolverle el acceso a ${nombre}?`,
              confirmar: 'Sí, activar',
          }
        : {
              titulo: 'Desactivar cuenta',
              mensaje: `¿Quieres quitarle el acceso a ${nombre}? No podrá iniciar sesión, pero sus datos y reservas se conservan.`,
              confirmar: 'Sí, desactivar',
          };
};

const AdminUsuarios = () => {
    const { usuario: sesion } = useAuth();
    const [filtros, setFiltros] = useState<FiltroUsuariosAdmin>({ q: '', rol: '', activo: '' });
    const [aplicados, setAplicados] = useState<FiltroUsuariosAdmin>({});
    const [pagina, setPagina] = useState(0);
    const [resultado, setResultado] = useState<{
        clave: string;
        datos: PaginaUsuarios | null;
        error: string | null;
    } | null>(null);
    const [errorAccion, setErrorAccion] = useState<string | null>(null);
    const [aviso, setAviso] = useState<string | null>(null);
    const [pendiente, setPendiente] = useState<Accion | null>(null);
    const [procesando, setProcesando] = useState(false);
    const [recarga, setRecarga] = useState(0);

    // La consulta en curso se deduce de la clave: mientras no coincida con el último resultado, se está cargando.
    const claveActual = JSON.stringify([aplicados, pagina, recarga]);
    const cargando = resultado?.clave !== claveActual;
    const datos = resultado?.datos ?? null;
    const error = errorAccion ?? (resultado?.clave === claveActual ? resultado.error : null);

    useEffect(() => {
        let vigente = true;
        listarUsuariosAdmin({ ...aplicados, pagina, tamano: TAMANO_PAGINA })
            .then((pagina_) => {
                if (vigente) setResultado({ clave: claveActual, datos: pagina_, error: null });
            })
            .catch((err: unknown) => {
                if (vigente)
                    setResultado((previo) => ({
                        clave: claveActual,
                        datos: previo?.datos ?? null,
                        error: obtenerMensajeError(err, 'No pudimos cargar los usuarios. Intenta de nuevo.'),
                    }));
            });
        return () => {
            vigente = false;
        };
    }, [aplicados, pagina, recarga, claveActual]);

    const handleBuscar = (evento: FormEvent<HTMLFormElement>) => {
        evento.preventDefault();
        setAviso(null);
        setErrorAccion(null);
        setPagina(0);
        setAplicados({ ...filtros });
    };

    const handleLimpiar = () => {
        setFiltros({ q: '', rol: '', activo: '' });
        setAviso(null);
        setPagina(0);
        setAplicados({});
    };

    const cancelar = useCallback(() => setPendiente(null), []);

    const confirmar = async () => {
        if (!pendiente) return;
        setProcesando(true);
        try {
            if (pendiente.tipo === 'rol') {
                await cambiarRolUsuario(pendiente.usuario.id, pendiente.rol);
                setAviso(`${nombreCompleto(pendiente.usuario)} ahora es ${ETIQUETA_ROL[pendiente.rol].toLowerCase()}.`);
            } else {
                await cambiarActivoUsuario(pendiente.usuario.id, pendiente.activo);
                setAviso(
                    pendiente.activo
                        ? `La cuenta de ${nombreCompleto(pendiente.usuario)} quedó activa.`
                        : `La cuenta de ${nombreCompleto(pendiente.usuario)} quedó desactivada.`,
                );
            }
            setErrorAccion(null);
            setRecarga((n) => n + 1);
        } catch (err) {
            setAviso(null);
            setErrorAccion(obtenerMensajeError(err, 'No pudimos guardar el cambio. Intenta de nuevo.'));
        } finally {
            setProcesando(false);
            setPendiente(null);
        }
    };

    return (
        <section aria-labelledby="admin-usuarios-titulo">
            <h1 id="admin-usuarios-titulo" className="admin__h1">
                Usuarios
            </h1>
            <p className="admin__ayuda">
                Busca personas, cambia su rol o retírales el acceso. Los cambios quedan registrados en la auditoría.
            </p>

            <form className="admin-filtros" onSubmit={handleBuscar} role="search" aria-label="Buscar usuarios">
                <label className="admin-campo admin-campo--ancho" htmlFor="usuarios-q">
                    <span>Nombre o correo</span>
                    <input
                        id="usuarios-q"
                        type="search"
                        value={filtros.q ?? ''}
                        onChange={(e) => setFiltros({ ...filtros, q: e.target.value })}
                    />
                </label>
                <label className="admin-campo" htmlFor="usuarios-rol">
                    <span>Rol</span>
                    <select
                        id="usuarios-rol"
                        value={filtros.rol ?? ''}
                        onChange={(e) => setFiltros({ ...filtros, rol: e.target.value as RolUsuario | '' })}
                    >
                        <option value="">Todos</option>
                        <option value="ROLE_ADMIN">Administradores</option>
                        <option value="ROLE_CLIENTE">Clientes</option>
                    </select>
                </label>
                <label className="admin-campo" htmlFor="usuarios-estado">
                    <span>Estado</span>
                    <select
                        id="usuarios-estado"
                        value={filtros.activo === '' || filtros.activo === undefined ? '' : String(filtros.activo)}
                        onChange={(e) =>
                            setFiltros({ ...filtros, activo: e.target.value === '' ? '' : e.target.value === 'true' })
                        }
                    >
                        <option value="">Todos</option>
                        <option value="true">Activos</option>
                        <option value="false">Desactivados</option>
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
            {error && (
                <p className="admin-error" role="alert">
                    {error}
                </p>
            )}

            {cargando && !datos && <p role="status">Cargando usuarios…</p>}
            {datos && datos.contenido.length === 0 && !cargando && (
                <p className="admin-vacio">No encontramos usuarios con esos filtros.</p>
            )}

            {datos && datos.contenido.length > 0 && (
                <div className="admin-tabla__contenedor" aria-busy={cargando}>
                    <table className="admin-tabla">
                        <caption className="sr-only">Lista de usuarios</caption>
                        <thead>
                            <tr>
                                <th scope="col">Nombre</th>
                                <th scope="col">Correo</th>
                                <th scope="col">Rol</th>
                                <th scope="col">Estado</th>
                                <th scope="col">Registro</th>
                                <th scope="col">Acciones</th>
                            </tr>
                        </thead>
                        <tbody>
                            {datos.contenido.map((u) => {
                                const esYo = u.id === sesion?.id;
                                return (
                                    <tr key={u.id}>
                                        <td>
                                            <div className="admin-tabla__persona">
                                                <span className="admin-avatar admin-avatar--pequeno" aria-hidden="true">
                                                    {iniciales(nombreCompleto(u))}
                                                </span>
                                                <strong>
                                                    {nombreCompleto(u)}
                                                    {esYo && <span className="admin-etiqueta"> (tú)</span>}
                                                </strong>
                                            </div>
                                        </td>
                                        <td>{u.correo}</td>
                                        <td>
                                            <span
                                                className={`admin-rol ${u.rol === 'ROLE_ADMIN' ? 'admin-rol--admin' : ''}`}
                                            >
                                                {ETIQUETA_ROL[u.rol]}
                                            </span>
                                        </td>
                                        <td>
                                            <span
                                                className={`admin-estado admin-estado--${u.activo ? 'activo' : 'inactivo'}`}
                                            >
                                                {u.activo ? 'Activo' : 'Desactivado'}
                                            </span>
                                        </td>
                                        <td>{formatearFecha(u.fechaRegistro)}</td>
                                        <td>
                                            {esYo ? (
                                                <span className="admin-etiqueta">No puedes cambiar tu cuenta</span>
                                            ) : (
                                                <div className="admin-tabla__acciones">
                                                    <BotonAccion
                                                        icono={u.rol === 'ROLE_ADMIN' ? 'usuario' : 'escudo'}
                                                        texto={
                                                            u.rol === 'ROLE_ADMIN'
                                                                ? 'Hacer cliente'
                                                                : 'Hacer administrador'
                                                        }
                                                        etiqueta={`${u.rol === 'ROLE_ADMIN' ? 'Hacer cliente a' : 'Hacer administrador a'} ${nombreCompleto(u)}`}
                                                        onClick={() =>
                                                            setPendiente({
                                                                tipo: 'rol',
                                                                usuario: u,
                                                                rol:
                                                                    u.rol === 'ROLE_ADMIN'
                                                                        ? 'ROLE_CLIENTE'
                                                                        : 'ROLE_ADMIN',
                                                            })
                                                        }
                                                    />
                                                    <BotonAccion
                                                        icono={u.activo ? 'ban' : 'check'}
                                                        texto={u.activo ? 'Desactivar' : 'Activar'}
                                                        etiqueta={`${u.activo ? 'Desactivar' : 'Activar'} la cuenta de ${nombreCompleto(u)}`}
                                                        peligro={u.activo}
                                                        onClick={() =>
                                                            setPendiente({
                                                                tipo: 'activo',
                                                                usuario: u,
                                                                activo: !u.activo,
                                                            })
                                                        }
                                                    />
                                                </div>
                                            )}
                                        </td>
                                    </tr>
                                );
                            })}
                        </tbody>
                    </table>
                </div>
            )}

            {datos && (
                <PaginacionAdmin
                    etiqueta="Paginación de usuarios"
                    pagina={pagina}
                    tamano={datos.tamano}
                    total={datos.total}
                    cargando={cargando}
                    singular="usuario"
                    plural="usuarios"
                    onCambiar={setPagina}
                />
            )}

            {pendiente && (
                <DialogoConfirmacion
                    {...(() => {
                        const d = describirAccion(pendiente);
                        return { titulo: d.titulo, mensaje: d.mensaje, textoConfirmar: d.confirmar };
                    })()}
                    peligro={
                        (pendiente.tipo === 'activo' && !pendiente.activo) ||
                        (pendiente.tipo === 'rol' && pendiente.rol === 'ROLE_CLIENTE')
                    }
                    procesando={procesando}
                    onConfirmar={confirmar}
                    onCancelar={cancelar}
                />
            )}
        </section>
    );
};

export default AdminUsuarios;
