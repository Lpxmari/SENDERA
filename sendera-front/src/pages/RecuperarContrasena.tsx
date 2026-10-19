import { FormEvent, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import CampoTexto from '../components/auth/CampoTexto';
import { obtenerMensajeError, solicitarRecuperacion } from '../services/authService';
import { validarCorreo } from '../utils/validaciones';

const RecuperarContrasena = () => {
    const navigate = useNavigate();
    const [correo, setCorreo] = useState('');
    const [error, setError] = useState<string | null>(null);
    const [errorGeneral, setErrorGeneral] = useState<string | null>(null);
    const [cargando, setCargando] = useState(false);

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault();
        const errorCorreo = validarCorreo(correo);
        setError(errorCorreo);
        setErrorGeneral(null);
        if (errorCorreo) return;

        setCargando(true);
        try {
            await solicitarRecuperacion({ correo: correo.trim() });
            navigate('/cambiar-contrasena', { state: { correo: correo.trim() } });
        } catch (err) {
            setErrorGeneral(obtenerMensajeError(err, 'No se pudo enviar el código. Intenta de nuevo.'));
        } finally {
            setCargando(false);
        }
    };

    return (
        <>
            <div className="auth-encabezado">
                <h1 className="auth-titulo">Recuperar contraseña</h1>
                <p className="auth-subtitulo">
                    Ingresa el correo electrónico asociado a tu cuenta y te enviaremos un código de 6 dígitos.
                </p>
            </div>

            <form className="auth-form" onSubmit={handleSubmit} noValidate>
                {errorGeneral && <p className="auth-alerta" role="alert">{errorGeneral}</p>}
                <CampoTexto etiqueta="Correo electrónico" type="email" autoComplete="email"
                            placeholder="Ingresa tu correo" valor={correo} onCambio={setCorreo} error={error} />
                <button type="submit" className="auth-boton" disabled={cargando}>
                    {cargando ? 'Enviando…' : 'Enviar código'}
                </button>
                <Link to="/login" className="auth-enlace auth-enlace--centro">← Volver a iniciar sesión</Link>
            </form>
        </>
    );
};

export default RecuperarContrasena;
