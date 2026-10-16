import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

/**
 * Oculta las vistas de administración a quien no es administrador. Es solo experiencia de uso:
 * la seguridad real está en el backend, que responde 401/403 en cada /admin/**.
 */
const RutaAdmin = () => {
    const { autenticado, esAdmin } = useAuth();
    const ubicacion = useLocation();
    if (!autenticado) return <Navigate to="/login" replace state={{ desde: ubicacion.pathname + ubicacion.search }} />;
    if (!esAdmin) return <Navigate to="/" replace />;
    return <Outlet />;
};

export default RutaAdmin;
