import { FormEvent, useState } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import CampoTexto from '../components/auth/CampoTexto';
import { useAuth } from '../context/AuthContext';
import { obtenerMensajeError } from '../services/authService';
import { validarCorreo } from '../utils/validaciones';

const Login = () => {
    const { login, autenticado } = useAuth();
    const navigate = useNavigate();
    const ubicacion = useLocation();
    const destino = (ubicacion.state as { desde?: string; mensaje?: string } | null)?.desde ?? '/';
    const mensaje = (ubicacion.state as { desde?: string; mensaje?: string } | null)?.mensaje ?? null;

    const [correo, setCorreo] = useState('');
    const [contrasena, setContrasena] = useState('');
    const [errores, setErrores] = useState<{ correo?: string | null; contrasena?: string | null }>({});
    const [errorGeneral, setErrorGeneral] = useState<string | null>(null);
    const [cargando, setCargando] = useState(false);

    if (autenticado) return <Navigate to={destino} replace />;

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault();
        const nuevos = {
            correo: validarCorreo(correo),
            contrasena: contrasena ? null : 'Ingresa tu contraseña.',
        };
        setErrores(nuevos);
        setErrorGeneral(null);
        if (nuevos.correo || nuevos.contrasena) return;

        setCargando(true);
        try {
            await login({ correo: correo.trim(), contrasena });
            navigate(destino, { replace: true });
        } catch (err) {
            setErrorGeneral(obtenerMensajeError(err, 'Correo o contraseña incorrectos.'));
        } finally {
            setCargando(false);
        }
    };

    return (
        <>
            <div className="auth-encabezado">
                <p className="auth-bienvenida">Bienvenido<br />Para Acceder a la App Ingresa tus datos</p>
            </div>

            <form className="auth-form" onSubmit={handleSubmit} noValidate>
                {mensaje && <p className="auth-exito" role="status">{mensaje}</p>}
                {errorGeneral && <p className="auth-alerta" role="alert">{errorGeneral}</p>}
                <CampoTexto
                    etiqueta="Ingresa tu correo:"
                    type="email"
                    autoComplete="email"
                    placeholder="ej. maricz@"
                    valor={correo}
                    onCambio={setCorreo}
                    error={errores.correo}
                />
                <CampoTexto
                    etiqueta="Ingresa tu contraseña:"
                    type="password"
                    autoComplete="current-password"
                    placeholder="ej. 12eo1nd"
                    valor={contrasena}
                    onCambio={setContrasena}
                    error={errores.contrasena}
                />
                <button type="submit" className="auth-boton" disabled={cargando}>
                    {cargando ? 'Ingresando…' : 'Iniciar Sesión'}
                </button>
                <Link to="/recuperar-contrasena" className="auth-enlace auth-enlace--centro">
                    Si olvidaste tu contraseña da click aquí
                </Link>
                <div className="auth-separador" role="separator"><span>o</span></div>
                <Link to="/registro" className="auth-boton auth-boton--contorno">Registrarse</Link>
            </form>
        </>
    );
};

export default Login;
