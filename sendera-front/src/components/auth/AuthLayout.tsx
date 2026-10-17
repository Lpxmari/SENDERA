import { Link, Outlet } from 'react-router-dom';
import logoSendera from '../../assets/logo-sendera.png';

/** Contenedor centrado y sin encabezado de búsqueda para las vistas de autenticación. */
const AuthLayout = () => (
    <div className="auth-pagina">
        <div className="auth-tarjeta">
            <Link to="/" className="auth-logo" aria-label="Sendera, ir al inicio">
                <img src={logoSendera} alt="Sendera agente de turismo" />
            </Link>
            <Outlet />
        </div>
    </div>
);

export default AuthLayout;
