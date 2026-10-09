import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

/** Redirige a /login si no hay sesión, recordando la ruta solicitada. */
const RutaProtegida = () => {
    const { autenticado } = useAuth();
    const ubicacion = useLocation();
    if (!autenticado) return <Navigate to="/login" replace state={{ desde: ubicacion.pathname + ubicacion.search }} />;
    return <Outlet />;
};

export default RutaProtegida;
