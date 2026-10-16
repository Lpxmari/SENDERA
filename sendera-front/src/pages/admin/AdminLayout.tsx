import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom';
import logoSendera from '../../assets/logo-sendera.png';
import { useAuth } from '../../context/AuthContext';
import '../../admin.css';

const SECCIONES = [
    { ruta: '/admin/usuarios', etiqueta: 'Usuarios' },
    { ruta: '/admin/destinos', etiqueta: 'Destinos' },
];

/** Estructura común de la administración: barra superior y menú lateral. */
const AdminLayout = () => {
    const { usuario, logout } = useAuth();
    const navigate = useNavigate();

    const handleCerrarSesion = () => {
        logout();
        navigate('/');
    };

    return (
        <div className="admin">
            <header className="admin__barra">
                <Link to="/admin" className="admin__logo" aria-label="Sendera, panel de administración">
                    <img src={logoSendera} alt="Sendera agente de turismo" />
                </Link>
                <p className="admin__titulo">Panel de administración</p>
                <div className="admin__sesion">
                    <Link to="/">Ver el sitio</Link>
                    <span>{usuario?.nombre}</span>
                    <button type="button" className="boton boton--verde" onClick={handleCerrarSesion}>
                        Cerrar sesión
                    </button>
                </div>
            </header>

            <div className="admin__cuerpo">
                <nav className="admin__menu" aria-label="Administración">
                    {SECCIONES.map((seccion) => (
                        <NavLink
                            key={seccion.ruta}
                            to={seccion.ruta}
                            className={({ isActive }) => `admin__enlace ${isActive ? 'admin__enlace--activo' : ''}`}
                        >
                            {seccion.etiqueta}
                        </NavLink>
                    ))}
                </nav>
                <main className="admin__contenido">
                    <Outlet />
                </main>
            </div>
        </div>
    );
};

export default AdminLayout;
