import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

/** Vistas pensadas para viajeros (como "Mi perfil"): un administrador va directo a su panel. */
const RutaCliente = () => {
    const { esAdmin } = useAuth();
    if (esAdmin) return <Navigate to="/admin" replace />;
    return <Outlet />;
};

export default RutaCliente;
