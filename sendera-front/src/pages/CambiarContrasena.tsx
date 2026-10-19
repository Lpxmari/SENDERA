import { FormEvent, useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import CampoTexto from '../components/auth/CampoTexto';
import {
    cambiarContrasena,
    obtenerMensajeError,
    solicitarRecuperacion,
    verificarCodigo,
} from '../services/authService';
import { validarCodigo, validarContrasenaNueva, validarCoincidencia } from '../utils/validaciones';

type Paso = 'codigo' | 'contrasena';

const CambiarContrasena = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const correoInicial = (location.state as { correo?: string } | null)?.correo ?? '';

    const [correo, setCorreo] = useState(correoInicial);
    const [paso, setPaso] = useState<Paso>('codigo');
    const [codigo, setCodigo] = useState('');
    const [nueva, setNueva] = useState('');
    const [confirmacion, setConfirmacion] = useState('');
    const [errores, setErrores] = useState<{ nueva?: string | null; confirmacion?: string | null }>({});
    const [errorCodigo, setErrorCodigo] = useState<string | null>(null);
    const [errorGeneral, setErrorGeneral] = useState<string | null>(null);
    const [mensajeExito, setMensajeExito] = useState<string | null>(null);
    const [cargando, setCargando] = useState(false);
    const [reenviando, setReenviando] = useState(false);
    const [cuentaRegresiva, setCuentaRegresiva] = useState(60);

    useEffect(() => {
        if (paso !== 'codigo' || cuentaRegresiva <= 0) return;
        const temporizador = window.setInterval(() => setCuentaRegresiva((actual) => (actual > 0 ? actual - 1 : 0)), 1000);
        return () => window.clearInterval(temporizador);
    }, [paso, cuentaRegresiva]);

    const correoValido = correo.trim();
    const requiereCorreo = !correoInicial;

    const handleVerificarCodigo = async (e: FormEvent) => {
        e.preventDefault();
        const correoParaUsar = correo.trim();
        const error = validarCodigo(codigo);
        setErrorCodigo(error);
        setErrorGeneral(null);
        if (!correoParaUsar || error) return;

        setCargando(true);
        try {
            await verificarCodigo({ correo: correoParaUsar, codigo });
            setPaso('contrasena');
            setMensajeExito('Código correcto. Ahora crea tu nueva contraseña.');
            setErrorGeneral(null);
        } catch (err) {
            setErrorGeneral(obtenerMensajeError(err, 'El código no es correcto o ya venció. Revisa el último correo que te enviamos.'));
        } finally {
            setCargando(false);
        }
    };

    const handleReenviarCodigo = async () => {
        if (!correoValido || reenviando || cuentaRegresiva > 0) return;
        setReenviando(true);
        setErrorGeneral(null);
        try {
            await solicitarRecuperacion({ correo: correoValido });
            setCuentaRegresiva(60);
            setMensajeExito('Te enviamos un código nuevo.');
            setCodigo('');
            setErrorCodigo(null);
        } catch (err) {
            setErrorGeneral(obtenerMensajeError(err, 'No se pudo reenviar el código. Intenta de nuevo.'));
        } finally {
            setReenviando(false);
        }
    };

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
            await cambiarContrasena({ correo: correoValido, codigo, nuevaContrasena: nueva });
            navigate('/login', { replace: true, state: { mensaje: 'Tu contraseña se cambió. Ya puedes iniciar sesión.' } });
        } catch (err) {
            setErrorGeneral(obtenerMensajeError(err, 'No se pudo cambiar la contraseña. Intenta de nuevo.'));
            setPaso('codigo');
        } finally {
            setCargando(false);
        }
    };

    return (
        <>
            <div className="auth-encabezado">
                <h1 className="auth-titulo">Cambiar contraseña</h1>
            </div>

            {paso === 'codigo' ? (
                <form className="auth-form" onSubmit={handleVerificarCodigo} noValidate>
                    {errorGeneral && <p className="auth-alerta" role="alert">{errorGeneral}</p>}
                    {mensajeExito && <p className="auth-exito" role="status">{mensajeExito}</p>}
                    {requiereCorreo && (
                        <CampoTexto
                            etiqueta="Correo electrónico"
                            type="email"
                            autoComplete="email"
                            placeholder="Ingresa tu correo"
                            valor={correo}
                            onCambio={setCorreo}
                            error={correoValido ? null : 'Ingresa tu correo electrónico.'}
                        />
                    )}
                    {!requiereCorreo && (
                        <p className="auth-subtitulo">
                            Te enviamos un código de 6 dígitos a <strong>{correo}</strong>. Revisa también la carpeta de spam.
                        </p>
                    )}
                    <CampoTexto
                        etiqueta="Código de verificación"
                        type="text"
                        inputMode="numeric"
                        autoComplete="one-time-code"
                        maxLength={6}
                        placeholder="123456"
                        valor={codigo}
                        onCambio={(valor) => setCodigo(valor.replace(/\D/g, '').slice(0, 6))}
                        error={errorCodigo}
                    />
                    <button type="submit" className="auth-boton" disabled={cargando || !correoValido}>
                        {cargando ? 'Verificando…' : 'Verificar código'}
                    </button>
                    <button
                        type="button"
                        className="auth-enlace auth-enlace--centro"
                        disabled={reenviando || cuentaRegresiva > 0 || !correoValido}
                        onClick={handleReenviarCodigo}
                        style={{ background: 'transparent', border: 'none', padding: 0 }}
                    >
                        {cuentaRegresiva > 0 ? `Reenviar código en ${cuentaRegresiva} s` : reenviando ? 'Enviando…' : 'Reenviar código'}
                    </button>
                    <button type="button" className="auth-enlace auth-enlace--centro"
                            onClick={() => navigate('/recuperar-contrasena')} style={{ background: 'transparent', border: 'none', padding: 0 }}>
                        ¿No es tu correo? Cámbialo
                    </button>
                    <Link to="/login" className="auth-enlace auth-enlace--centro">← Volver a iniciar sesión</Link>
                </form>
            ) : (
                <form className="auth-form" onSubmit={handleSubmit} noValidate>
                    {errorGeneral && <p className="auth-alerta" role="alert">{errorGeneral}</p>}
                    {mensajeExito && <p className="auth-exito" role="status">{mensajeExito}</p>}
                    <CampoTexto
                        etiqueta="Nueva contraseña"
                        type="password"
                        autoComplete="new-password"
                        placeholder="Ingresa tu nueva contraseña"
                        valor={nueva}
                        onCambio={setNueva}
                        error={errores.nueva}
                    />
                    <CampoTexto
                        etiqueta="Confirmar contraseña"
                        type="password"
                        autoComplete="new-password"
                        placeholder="Repite tu nueva contraseña"
                        valor={confirmacion}
                        onCambio={setConfirmacion}
                        error={errores.confirmacion}
                        ayuda="La contraseña debe ser fácil de recordar y segura."
                    />
                    <button type="submit" className="auth-boton" disabled={cargando}>
                        {cargando ? 'Guardando…' : 'Cambiar contraseña'}
                    </button>
                    <button type="button" className="auth-enlace auth-enlace--centro"
                            onClick={() => { setPaso('codigo'); setErrorGeneral(null); }} style={{ background: 'transparent', border: 'none', padding: 0 }}>
                        ← Volver al paso anterior
                    </button>
                    <Link to="/login" className="auth-enlace auth-enlace--centro">← Volver a iniciar sesión</Link>
                </form>
            )}
        </>
    );
};

export default CambiarContrasena;
