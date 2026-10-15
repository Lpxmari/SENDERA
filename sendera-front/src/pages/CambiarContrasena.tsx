import { FormEvent, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import CampoTexto from '../components/auth/CampoTexto';
import { cambiarContrasena, obtenerMensajeError } from '../services/authService';
import { validarContrasenaNueva, validarCoincidencia } from '../utils/validaciones';

/** Se abre desde el enlace del correo: /cambiar-contrasena?token=... */
const CambiarContrasena = () => {
    const [params] = useSearchParams();
    const token = params.get('token') ?? '';
    const navigate = useNavigate();

    const [nueva, setNueva] = useState('');
    const [confirmacion, setConfirmacion] = useState('');
    const [errores, setErrores] = useState<{ nueva?: string | null; confirmacion?: string | null }>({});
    const [errorGeneral, setErrorGeneral] = useState<string | null>(null);
    const [cargando, setCargando] = useState(false);

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault();
        const nuevos = {
            nueva: validarContrasenaNueva(nueva),
            confirmacion: validarCoincidencia(nueva, confirmacion),
        };
        setErrores(nuevos);
        setErrorGeneral(null);
        if (nuevos.nueva || nuevos.confirmacion) return;

        setCargando(true);
        try {
            await cambiarContrasena({ token, nuevaContrasena: nueva });
            navigate('/login', { replace: true });
        } catch (err) {
            setErrorGeneral(obtenerMensajeError(err, 'El enlace no es válido o ya expiró. Solicita uno nuevo.'));
        } finally {
            setCargando(false);
        }
    };

    return (
        <>
            <div className="auth-encabezado">
                <h1 className="auth-titulo">Cambiar contraseña</h1>
                <p className="auth-subtitulo">Crea una nueva contraseña para proteger tu cuenta.</p>
            </div>

            {!token ? (
                <div className="auth-form">
                    <p className="auth-alerta" role="alert">
                        El enlace no es válido. Solicita uno nuevo para cambiar tu contraseña.
                    </p>
                    <Link to="/recuperar-contrasena" className="auth-enlace auth-enlace--centro">Solicitar nuevo enlace</Link>
                </div>
            ) : (
                <form className="auth-form" onSubmit={handleSubmit} noValidate>
                    {errorGeneral && <p className="auth-alerta" role="alert">{errorGeneral}</p>}
                    <CampoTexto etiqueta="Nueva contraseña" type="password" autoComplete="new-password"
                                placeholder="Ingresa tu nueva contraseña" valor={nueva} onCambio={setNueva} error={errores.nueva} />
                    <CampoTexto etiqueta="Confirmar contraseña" type="password" autoComplete="new-password"
                                placeholder="Repite tu nueva contraseña" valor={confirmacion} onCambio={setConfirmacion}
                                error={errores.confirmacion} ayuda="La contraseña debe ser fácil de recordar y segura." />
                    <button type="submit" className="auth-boton" disabled={cargando}>
                        {cargando ? 'Guardando…' : 'Cambiar contraseña'}
                    </button>
                    <Link to="/login" className="auth-enlace auth-enlace--centro">← Volver a iniciar sesión</Link>
                </form>
            )}
        </>
    );
};

export default CambiarContrasena;
