import { BrowserRouter as Router, Routes, Route, Outlet } from 'react-router-dom';
import Encabezado from './components/Encabezado';
import AuthLayout from './components/auth/AuthLayout';
import RutaProtegida from './components/auth/RutaProtegida';
import { AuthProvider } from './context/AuthContext';
import Home from './pages/Home';
import ResultadosBusqueda from './pages/ResultadosBusqueda';
import DetalleDestino from './pages/DetalleDestino';
import FormularioReserva from './pages/FormularioReserva';
import ConfirmacionReserva from './pages/ConfirmacionReserva';
import Login from './pages/Login';
import Registro from './pages/Registro';
import RecuperarContrasena from './pages/RecuperarContrasena';
import CambiarContrasena from './pages/CambiarContrasena';
import './App.css';
import './auth.css';

const LayoutPrincipal = () => (
    <div className="app-container">
        <Encabezado />
        <main>
            <Outlet />
        </main>
    </div>
);

function App() {
    return (
        <AuthProvider>
            <Router>
                <Routes>
                    {/* Vistas públicas de autenticación (sin encabezado de búsqueda) */}
                    <Route element={<AuthLayout />}>
                        <Route path="/login" element={<Login />} />
                        <Route path="/registro" element={<Registro />} />
                        <Route path="/recuperar-contrasena" element={<RecuperarContrasena />} />
                        <Route path="/cambiar-contrasena" element={<CambiarContrasena />} />
                    </Route>

                    <Route element={<LayoutPrincipal />}>
                        <Route path="/" element={<Home />} />
                        <Route path="/resultados" element={<ResultadosBusqueda />} />
                        <Route path="/destinos/:id" element={<DetalleDestino />} />

                        {/* Requieren sesión iniciada */}
                        <Route element={<RutaProtegida />}>
                            <Route path="/reservar/:destinoId" element={<FormularioReserva />} />
                            <Route path="/confirmacion" element={<ConfirmacionReserva />} />
                        </Route>
                    </Route>
                </Routes>
            </Router>
        </AuthProvider>
    );
}

export default App;
