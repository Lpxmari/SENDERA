import { FormEvent, useState } from 'react';
import { Link } from 'react-router-dom';
import CampoTexto from '../components/auth/CampoTexto';
import { obtenerMensajeError, solicitarRecuperacion } from '../services/authService';
import { validarCorreo } from '../utils/validaciones';

const RecuperarContrasena = () => {
    const [correo, setCorreo] = useState('');
    const [error, setError] = useState<string | null>(null);
    const [errorGeneral, setErrorGeneral] = useState<string | null>(null);
    const [enviado, setEnviado] = useState(false);
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
            setEnviado(true);
        } catch (err) {
            setErrorGeneral(obtenerMensajeError(err, 'No se pudo enviar el correo. Intenta de nuevo.'));
        } finally {
            setCargando(false);
        }
    };

    return (
        <>
            <div className="auth-encabezado">
                <h1 className="auth-titulo">Recuperar contraseña</h1>
                <p className="auth-subtitulo">
                    Ingresa el correo electrónico asociado a tu cuenta y te enviaremos las instrucciones para recuperar tu contraseña.
                </p>
            </div>

            {enviado ? (
                <div className="auth-form">
                    <p className="auth-exito" role="status">
                        Si el correo está registrado, recibirás las instrucciones en unos minutos. Revisa también tu carpeta de spam.
                    </p>
                    <Link to="/login" className="auth-enlace auth-enlace--centro">← Volver a iniciar sesión</Link>
                </div>
            ) : (
                <form className="auth-form" onSubmit={handleSubmit} noValidate>
                    {errorGeneral && <p className="auth-alerta" role="alert">{errorGeneral}</p>}
                    <CampoTexto etiqueta="Correo electrónico" type="email" autoComplete="email"
                                placeholder="Ingresa tu correo" valor={correo} onCambio={setCorreo} error={error} />
                    <button type="submit" className="auth-boton" disabled={cargando}>
                        {cargando ? 'Enviando…' : 'Enviar instrucciones'}
                    </button>
                    <Link to="/login" className="auth-enlace auth-enlace--centro">← Volver a iniciar sesión</Link>
                </form>
            )}
        </>
    );
};

export default RecuperarContrasena;
