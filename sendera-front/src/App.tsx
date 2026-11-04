import type { ComponentProps } from 'react';
import { BrowserRouter as Router, Routes, Route, Outlet, Navigate } from 'react-router-dom';
import Encabezado from './components/Encabezado';
import AuthLayout from './components/auth/AuthLayout';
import RutaProtegida from './components/auth/RutaProtegida';
import RutaAdmin from './components/auth/RutaAdmin';
import RutaCliente from './components/auth/RutaCliente';
import { AuthProvider } from './context/AuthContext';
import Home from './pages/Home';
import ResultadosBusqueda from './pages/ResultadosBusqueda';
import DetalleDestino from './pages/DetalleDestino';
import FormularioReserva from './pages/FormularioReserva';
import ConfirmacionReserva from './pages/ConfirmacionReserva';
import Perfil from './pages/Perfil';
import CrearItinerario from './pages/CrearItinerario';
import DetalleItinerario from './pages/DetalleItinerario';
import ListaItinerarios from './pages/ListaItinerarios';
import AdminLayout from './pages/admin/AdminLayout';
import AdminUsuarios from './pages/admin/AdminUsuarios';
import AdminDestinos from './pages/admin/AdminDestinos';
import AdminReservas from './pages/admin/AdminReservas';
import AdminResenas from './pages/admin/AdminResenas';
import Login from './pages/Login';
import Registro from './pages/Registro';
import RecuperarContrasena from './pages/RecuperarContrasena';
import CambiarContrasena from './pages/CambiarContrasena';
import './App.css';
import './auth.css';

const LayoutPrincipal = ({ encabezado }: { encabezado?: ComponentProps<typeof Encabezado>['variante'] }) => (
    <div className="app-container">
        <Encabezado variante={encabezado} />
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

                    {/* Administración: la ruta se oculta por rol, pero el backend autoriza cada /admin/** */}
                    <Route element={<RutaAdmin />}>
                        <Route path="/admin" element={<AdminLayout />}>
                            <Route index element={<Navigate to="usuarios" replace />} />
                            <Route path="usuarios" element={<AdminUsuarios />} />
                            <Route path="destinos" element={<AdminDestinos />} />
                            <Route path="reservas" element={<AdminReservas />} />
                            <Route path="resenas" element={<AdminResenas />} />
                        </Route>
                    </Route>

                    <Route element={<LayoutPrincipal />}>
                        <Route path="/" element={<Home />} />
                        <Route path="/resultados" element={<ResultadosBusqueda />} />
                        <Route path="/destinos/:id" element={<DetalleDestino />} />

                        {/* Requieren sesión iniciada */}
                        <Route element={<RutaProtegida />}>
                            <Route path="/reservar/:destinoId" element={<FormularioReserva />} />
                            <Route element={<RutaCliente />}>
                                <Route path="/perfil" element={<Perfil />} />
                            </Route>
                            <Route path="/confirmacion" element={<ConfirmacionReserva />} />
                        </Route>
                    </Route>

                    {/* Itinerarios personales: encabezado sin buscador (lista y formulario) o solo con "Mi Perfil" (detalle) */}
                    <Route element={<LayoutPrincipal encabezado="itinerarios" />}>
                        <Route element={<RutaProtegida />}>
                            <Route element={<RutaCliente />}>
                                <Route path="/itinerarios" element={<ListaItinerarios />} />
                                <Route path="/itinerarios/nuevo" element={<CrearItinerario />} />
                                <Route path="/itinerarios/:id/editar" element={<CrearItinerario />} />
                            </Route>
                        </Route>
                    </Route>
                    <Route element={<LayoutPrincipal encabezado="detalle" />}>
                        <Route element={<RutaProtegida />}>
                            <Route element={<RutaCliente />}>
                                <Route path="/itinerarios/:id" element={<DetalleItinerario />} />
                            </Route>
                        </Route>
                    </Route>
                </Routes>
            </Router>
        </AuthProvider>
    );
}

export default App;
