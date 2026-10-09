import { FormEvent, useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import CampoTexto from '../components/auth/CampoTexto';
import { useAuth } from '../context/AuthContext';
import { obtenerMensajeError } from '../services/authService';
import { validarContrasenaNueva, validarCoincidencia, validarCorreo } from '../utils/validaciones';

type Errores = Partial<Record<'nombre' | 'apellidos' | 'correo' | 'contrasena' | 'confirmacion', string | null>>;

const Registro = () => {
    const { registro, autenticado } = useAuth();
    const navigate = useNavigate();

    const [nombre, setNombre] = useState('');
    const [apellidos, setApellidos] = useState('');
    const [correo, setCorreo] = useState('');
    const [contrasena, setContrasena] = useState('');
    const [confirmacion, setConfirmacion] = useState('');
    const [errores, setErrores] = useState<Errores>({});
    const [errorGeneral, setErrorGeneral] = useState<string | null>(null);
    const [cargando, setCargando] = useState(false);

    if (autenticado) return <Navigate to="/" replace />;

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault();
        const nuevos: Errores = {
            nombre: nombre.trim() ? null : 'Ingresa tu nombre.',
            apellidos: apellidos.trim() ? null : 'Ingresa tus apellidos.',
            correo: validarCorreo(correo),
            contrasena: validarContrasenaNueva(contrasena),
            confirmacion: validarCoincidencia(contrasena, confirmacion),
        };
        setErrores(nuevos);
        setErrorGeneral(null);
        if (Object.values(nuevos).some(Boolean)) return;

        setCargando(true);
        try {
            await registro({ nombre: nombre.trim(), apellidos: apellidos.trim(), correo: correo.trim(), contrasena });
            navigate('/', { replace: true });
        } catch (err) {
            setErrorGeneral(obtenerMensajeError(err, 'No se pudo completar el registro. Intenta de nuevo.'));
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
                {errorGeneral && <p className="auth-alerta" role="alert">{errorGeneral}</p>}
                <CampoTexto etiqueta="Ingresa tu nombre:" autoComplete="given-name" placeholder="ej. Maria"
                            valor={nombre} onCambio={setNombre} error={errores.nombre} />
                <CampoTexto etiqueta="Ingresa tus apellidos:" autoComplete="family-name" placeholder="ej. Gutierrez"
                            valor={apellidos} onCambio={setApellidos} error={errores.apellidos} />
                <CampoTexto etiqueta="Ingresa tu correo:" type="email" autoComplete="email" placeholder="ej. maricz@"
                            valor={correo} onCambio={setCorreo} error={errores.correo} />
                <CampoTexto etiqueta="Ingresa una contraseña:" type="password" autoComplete="new-password" placeholder="ej. 12eo1nd"
                            valor={contrasena} onCambio={setContrasena} error={errores.contrasena} />
                <CampoTexto etiqueta="Confirma la contraseña:" type="password" autoComplete="new-password" placeholder="ej. 12eo1nd"
                            valor={confirmacion} onCambio={setConfirmacion} error={errores.confirmacion} />
                <button type="submit" className="auth-boton" disabled={cargando}>
                    {cargando ? 'Registrando…' : 'Registrarse'}
                </button>
                <p className="auth-pie">
                    Si ya tienes una cuenta da click → <Link to="/login" className="auth-enlace">aquí</Link>
                </p>
            </form>
        </>
    );
};

export default Registro;
