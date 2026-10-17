import { useEffect, useState } from 'react';
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import logoSendera from '../../assets/logo-sendera.png';
import IconoAdmin, { NombreIconoAdmin } from '../../components/admin/IconoAdmin';
import MenuPerfilAdmin from '../../components/admin/MenuPerfilAdmin';
import { useAuth } from '../../context/AuthContext';
import { listarReservasAdmin } from '../../services/adminService';
import '../../admin.css';

interface Seccion {
    ruta: string;
    etiqueta: string;
    icono: NombreIconoAdmin;
    /** Muestra el número de reservas pendientes junto a la sección. */
    conteoPendientes?: boolean;
}

const SECCIONES: Seccion[] = [
    { ruta: '/admin/usuarios', etiqueta: 'Usuarios', icono: 'usuarios' },
    { ruta: '/admin/destinos', etiqueta: 'Destinos', icono: 'destinos' },
    { ruta: '/admin/reservas', etiqueta: 'Reservas', icono: 'reservas', conteoPendientes: true },
    { ruta: '/admin/resenas', etiqueta: 'Reseñas', icono: 'resenas' },
];

/** Estructura de la administración: barra lateral con la marca y el menú, y barra superior con el perfil. */
const AdminLayout = () => {
    const { logout } = useAuth();
    const navigate = useNavigate();
    const { pathname } = useLocation();
    const [pendientes, setPendientes] = useState<number>(0);

    // El número se vuelve a consultar al cambiar de sección; si falla, simplemente no se muestra.
    useEffect(() => {
        let vigente = true;
        listarReservasAdmin({ estado: 'PENDIENTE', pagina: 0, tamano: 1 })
            .then((pagina) => {
                if (vigente) setPendientes(pagina.total);
            })
            .catch(() => {
                if (vigente) setPendientes(0);
            });
        return () => {
            vigente = false;
        };
    }, [pathname]);

    const handleCerrarSesion = () => {
        logout();
        navigate('/');
    };

    return (
        <div className="admin">
            <aside className="admin-lateral">
                <Link to="/admin" className="admin-lateral__marca" aria-label="Sendera, panel de administración">
                    <img src={logoSendera} alt="" />
                    <span>
                        <strong>Sendera</strong>
                        <small>Panel de administración</small>
                    </span>
                </Link>

                <nav className="admin-lateral__menu" aria-label="Administración">
                    {SECCIONES.map((seccion) => (
                        <NavLink
                            key={seccion.ruta}
                            to={seccion.ruta}
                            className={({ isActive }) =>
                                `admin-lateral__enlace ${isActive ? 'admin-lateral__enlace--activo' : ''}`
                            }
                        >
                            <IconoAdmin nombre={seccion.icono} />
                            <span>{seccion.etiqueta}</span>
                            {seccion.conteoPendientes && pendientes > 0 && (
                                <span
                                    className="admin-lateral__insignia"
                                    aria-label={`${pendientes} ${pendientes === 1 ? 'pendiente' : 'pendientes'}`}
                                >
                                    {pendientes}
                                </span>
                            )}
                        </NavLink>
                    ))}
                </nav>

                <p className="admin-lateral__grupo">Accesos rápidos</p>
                <Link to="/" className="admin-lateral__enlace">
                    <IconoAdmin nombre="sitio" />
                    <span>Ver el sitio</span>
                </Link>

                <button type="button" className="admin-lateral__salir" onClick={handleCerrarSesion}>
                    <IconoAdmin nombre="salir" />
                    <span>Cerrar sesión</span>
                </button>
            </aside>

            <div className="admin__principal">
                <header className="admin__barra">
                    <p className="admin__titulo">Panel de administración</p>
                    <MenuPerfilAdmin />
                </header>
                <main className="admin__contenido">
                    <Outlet />
                </main>
            </div>
        </div>
    );
};

export default AdminLayout;
