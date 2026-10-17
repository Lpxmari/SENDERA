import { ChangeEvent, useEffect, useRef, useState } from 'react';
import DatoPerfil from '../components/perfil/DatoPerfil';
import TarjetaPerfil from '../components/perfil/TarjetaPerfil';
import { useAuth } from '../context/AuthContext';
import { actualizarPerfil, fetchHistorialViajes, fetchOpiniones, fetchPerfil } from '../services/perfilService';
import { obtenerMensajeError } from '../services/apiClient';
import { OpinionViajero, PerfilEditable, PerfilViajero, ViajeHistorial } from '../types/perfil';
import { formatearPrecio } from '../utils/formatearPrecio';
import { validarCorreo } from '../utils/validaciones';
import '../perfil.css';

type TarjetaActiva = 'personal' | 'viajero' | null;

const TIPOS_IMAGEN = ['image/jpeg', 'image/png', 'image/webp'];
const MAX_BYTES_FOTO = 2 * 1024 * 1024;

const MESES = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'];

/** ISO (YYYY-MM-DD) a "23 Mar 1965", sin depender del idioma del navegador. */
const formatearFecha = (iso: string): string => {
    const [anio, mes, dia] = iso.split('-').map(Number);
    return `${dia} ${MESES[mes - 1]} ${anio}`;
};

const aLista = (texto: string): string[] =>
    texto
        .split(',')
        .map((item) => item.trim())
        .filter(Boolean);

const iniciales = (perfil: PerfilViajero): string =>
    `${perfil.nombre.charAt(0)}${perfil.apellidos.charAt(0)}`.toUpperCase();

const aEditable = (p: PerfilViajero): PerfilEditable => ({
    numeroIdentificacion: p.numeroIdentificacion,
    nombre: p.nombre,
    apellidos: p.apellidos,
    fechaNacimiento: p.fechaNacimiento,
    correo: p.correo,
    paisResidencia: p.paisResidencia,
    destinosFavoritos: p.destinosFavoritos,
    intereses: p.intereses,
});

const hoyIso = (): string => new Date().toISOString().slice(0, 10);

const validarPersonal = (d: PerfilEditable): string | null => {
    if (!d.nombre.trim() || !d.apellidos.trim()) return 'Escribe tu nombre y tus apellidos.';
    if (!/^\d{5,15}$/.test(d.numeroIdentificacion.trim()))
        return 'El número de identificación debe tener entre 5 y 15 dígitos.';
    if (!d.fechaNacimiento) return 'Indica tu fecha de nacimiento.';
    if (d.fechaNacimiento > hoyIso()) return 'La fecha de nacimiento no puede ser futura.';
    return null;
};

const validarViajero = (d: PerfilEditable): string | null => {
    const errorCorreo = validarCorreo(d.correo);
    if (errorCorreo) return errorCorreo;
    if (!d.paisResidencia.trim()) return 'Indica tu país de residencia.';
    return null;
};

const Perfil = () => {
    const { usuario, actualizarSesion } = useAuth();
    const [perfil, setPerfil] = useState<PerfilViajero | null>(null);
    const [opiniones, setOpiniones] = useState<OpinionViajero[]>([]);
    const [historial, setHistorial] = useState<ViajeHistorial[] | null>(null);
    const [verHistorial, setVerHistorial] = useState(false);
    const [errorCarga, setErrorCarga] = useState<string | null>(null);
    const [activa, setActiva] = useState<TarjetaActiva>(null);
    const [borrador, setBorrador] = useState<PerfilEditable | null>(null);
    const [textoDestinos, setTextoDestinos] = useState('');
    const [textoIntereses, setTextoIntereses] = useState('');
    const [contrasenaActual, setContrasenaActual] = useState('');
    const [errorFormulario, setErrorFormulario] = useState<string | null>(null);
    const [aviso, setAviso] = useState<string | null>(null);
    const [fotoVista, setFotoVista] = useState<string | null>(null);
    const [errorFoto, setErrorFoto] = useState<string | null>(null);
    const inputFoto = useRef<HTMLInputElement>(null);

    useEffect(() => {
        if (!usuario) return;
        let activo = true;
        Promise.all([fetchPerfil(usuario), fetchOpiniones()])
            .then(([datosPerfil, datosOpiniones]) => {
                if (!activo) return;
                setPerfil(datosPerfil);
                setOpiniones(datosOpiniones);
            })
            .catch((error: unknown) => {
                if (activo) setErrorCarga(obtenerMensajeError(error, 'No pudimos cargar tu perfil. Intenta de nuevo.'));
            });
        return () => {
            activo = false;
        };
    }, [usuario]);

    useEffect(
        () => () => {
            if (fotoVista) URL.revokeObjectURL(fotoVista);
        },
        [fotoVista],
    );

    if (errorCarga)
        return (
            <p className="perfil perfil-error" role="alert">
                {errorCarga}
            </p>
        );
    if (!perfil || !usuario)
        return (
            <p className="perfil" role="status">
                Cargando tu perfil…
            </p>
        );

    const editar = (tarjeta: Exclude<TarjetaActiva, null>) => {
        setBorrador(aEditable(perfil));
        setTextoDestinos(perfil.destinosFavoritos.join(', '));
        setTextoIntereses(perfil.intereses.join(', '));
        setContrasenaActual('');
        setErrorFormulario(null);
        setAviso(null);
        setActiva(tarjeta);
    };

    const cancelar = () => {
        setActiva(null);
        setBorrador(null);
        setContrasenaActual('');
        setErrorFormulario(null);
    };

    const guardar = async () => {
        if (!borrador) return;
        const datos: PerfilEditable = {
            ...borrador,
            correo: borrador.correo.trim(),
            destinosFavoritos: aLista(textoDestinos),
            intereses: aLista(textoIntereses),
        };
        const mensaje = activa === 'personal' ? validarPersonal(datos) : validarViajero(datos);
        if (mensaje) {
            setErrorFormulario(mensaje);
            return;
        }
        if (cambiaElCorreo(datos) && !contrasenaActual) {
            setErrorFormulario('Para cambiar tu correo escribe tu contraseña actual.');
            return;
        }
        try {
            const resultado = await actualizarPerfil(usuario.id, datos, contrasenaActual);
            setPerfil(resultado.perfil);
            actualizarSesion(resultado.sesion);
            cancelar();
            setAviso('Tus cambios se guardaron.');
        } catch (error) {
            setErrorFormulario(obtenerMensajeError(error, 'No pudimos guardar tus cambios. Intenta de nuevo.'));
        }
    };

    const cambiaElCorreo = (datos: PerfilEditable): boolean =>
        datos.correo.trim().toLowerCase() !== perfil.correo.trim().toLowerCase();

    const cambiarCampo = (campo: keyof PerfilEditable) => (e: ChangeEvent<HTMLInputElement>) =>
        setBorrador((actual) => (actual ? { ...actual, [campo]: e.target.value } : actual));

    const handleFoto = (e: ChangeEvent<HTMLInputElement>) => {
        const archivo = e.target.files?.[0];
        if (!archivo) return;
        if (!TIPOS_IMAGEN.includes(archivo.type)) {
            setErrorFoto('Elige una imagen JPG, PNG o WEBP.');
            return;
        }
        if (archivo.size > MAX_BYTES_FOTO) {
            setErrorFoto('La imagen no puede superar los 2 MB.');
            return;
        }
        setErrorFoto(null);
        setFotoVista(URL.createObjectURL(archivo));
    };

    const handleHistorial = async () => {
        if (verHistorial) {
            setVerHistorial(false);
            return;
        }
        try {
            if (historial === null) setHistorial(await fetchHistorialViajes());
            setVerHistorial(true);
        } catch (error) {
            setErrorCarga(obtenerMensajeError(error, 'No pudimos cargar tu historial de viajes.'));
        }
    };

    const campo = (id: string, etiqueta: string, valor: string, clave: keyof PerfilEditable, extra: object = {}) => (
        <label className="perfil-campo" htmlFor={id}>
            <span>{etiqueta}</span>
            <input id={id} value={valor} onChange={cambiarCampo(clave)} {...extra} />
        </label>
    );

    const foto = fotoVista ?? perfil.fotoUrl;

    return (
        <div className="perfil">
            <h1 className="perfil__saludo">¡Hola, {perfil.nombre}! Aquí puedes gestionar toda tu información</h1>

            <section className="perfil-resumen" aria-label="Resumen del viajero">
                <div className="perfil-avatar">
                    {foto ? (
                        <img src={foto} alt={`Foto de perfil de ${perfil.nombre}`} />
                    ) : (
                        <span aria-hidden="true">{iniciales(perfil)}</span>
                    )}
                    <button
                        type="button"
                        className="perfil-avatar__camara"
                        onClick={() => inputFoto.current?.click()}
                        aria-label="Cambiar foto de perfil"
                    >
                        <svg
                            viewBox="0 0 24 24"
                            width="14"
                            height="14"
                            fill="none"
                            stroke="currentColor"
                            strokeWidth="2"
                            aria-hidden="true"
                        >
                            <path d="M4 8h3l2-3h6l2 3h3v11H4zM12 17a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7z" />
                        </svg>
                    </button>
                    <input
                        ref={inputFoto}
                        type="file"
                        accept={TIPOS_IMAGEN.join(',')}
                        hidden
                        onChange={handleFoto}
                        aria-label="Elegir foto de perfil"
                    />
                </div>
                <div className="perfil-resumen__texto">
                    <p className="perfil-resumen__usuario">{perfil.nombreUsuario}</p>
                    <p className="perfil-resumen__nivel">{perfil.nivelViajero}</p>
                </div>
                <button
                    type="button"
                    className="boton boton--verde perfil-resumen__historial"
                    onClick={handleHistorial}
                    aria-expanded={verHistorial}
                    aria-controls="perfil-historial"
                >
                    Consultar Historial de Viajes
                </button>
            </section>
            {errorFoto && (
                <p className="perfil-error" role="alert">
                    {errorFoto}
                </p>
            )}
            {aviso && (
                <p className="perfil-aviso" role="status">
                    {aviso}
                </p>
            )}

            {verHistorial && historial && (
                <section id="perfil-historial" className="perfil-historial" aria-labelledby="perfil-historial-titulo">
                    <h2 id="perfil-historial-titulo">Historial de viajes</h2>
                    {historial.length === 0 ? (
                        <p>Aún no tienes viajes reservados.</p>
                    ) : (
                        <ul>
                            {historial.map((viaje) => (
                                <li key={viaje.id}>
                                    <strong>{viaje.destino}</strong>
                                    <span>{formatearFecha(viaje.fechaReserva)}</span>
                                    <span>{formatearPrecio(viaje.montoTotal)}</span>
                                    <span
                                        className={`perfil-estado perfil-estado--${viaje.estadoReserva.toLowerCase()}`}
                                    >
                                        {viaje.estadoReserva}
                                    </span>
                                </li>
                            ))}
                        </ul>
                    )}
                </section>
            )}

            <div className="perfil-tarjetas">
                <TarjetaPerfil
                    id="perfil-personal"
                    titulo="Información Personal"
                    editando={activa === 'personal'}
                    error={errorFormulario}
                    onEditar={() => editar('personal')}
                    onCancelar={cancelar}
                    onGuardar={guardar}
                    vista={
                        <>
                            <DatoPerfil icono="identificacion" etiqueta="No. Identificación">
                                {perfil.numeroIdentificacion}
                            </DatoPerfil>
                            <DatoPerfil icono="persona" etiqueta="Nombre Completo">
                                {perfil.nombre}
                            </DatoPerfil>
                            <DatoPerfil icono="persona" etiqueta="Apellidos">
                                {perfil.apellidos}
                            </DatoPerfil>
                            <DatoPerfil icono="calendario" etiqueta="Fecha de Nacimiento">
                                {formatearFecha(perfil.fechaNacimiento)}
                            </DatoPerfil>
                        </>
                    }
                    formulario={
                        borrador && (
                            <>
                                {campo(
                                    'perfil-identificacion',
                                    'No. Identificación',
                                    borrador.numeroIdentificacion,
                                    'numeroIdentificacion',
                                    { inputMode: 'numeric' },
                                )}
                                {campo('perfil-nombre', 'Nombre', borrador.nombre, 'nombre', {
                                    autoComplete: 'given-name',
                                })}
                                {campo('perfil-apellidos', 'Apellidos', borrador.apellidos, 'apellidos', {
                                    autoComplete: 'family-name',
                                })}
                                {campo(
                                    'perfil-nacimiento',
                                    'Fecha de Nacimiento',
                                    borrador.fechaNacimiento,
                                    'fechaNacimiento',
                                    { type: 'date', max: hoyIso() },
                                )}
                            </>
                        )
                    }
                />

                <TarjetaPerfil
                    id="perfil-viajero"
                    titulo="Información de Viajero"
                    editando={activa === 'viajero'}
                    error={errorFormulario}
                    onEditar={() => editar('viajero')}
                    onCancelar={cancelar}
                    onGuardar={guardar}
                    vista={
                        <>
                            <DatoPerfil icono="correo" etiqueta="Correo">
                                {perfil.correo}
                            </DatoPerfil>
                            <DatoPerfil icono="ubicacion" etiqueta="País de Residencia">
                                {perfil.paisResidencia}
                            </DatoPerfil>
                            <DatoPerfil icono="mapa" etiqueta="Destinos Favoritos">
                                {perfil.destinosFavoritos.join(' - ') || 'Sin definir'}
                            </DatoPerfil>
                            <DatoPerfil icono="persona" etiqueta="Intereses">
                                {perfil.intereses.join(' - ') || 'Sin definir'}
                            </DatoPerfil>
                        </>
                    }
                    formulario={
                        borrador && (
                            <>
                                {campo('perfil-correo', 'Correo', borrador.correo, 'correo', {
                                    type: 'email',
                                    autoComplete: 'email',
                                })}
                                {cambiaElCorreo(borrador) && (
                                    <label className="perfil-campo" htmlFor="perfil-contrasena-actual">
                                        <span>Contraseña actual (necesaria para cambiar tu correo)</span>
                                        <input
                                            id="perfil-contrasena-actual"
                                            type="password"
                                            autoComplete="current-password"
                                            value={contrasenaActual}
                                            onChange={(e) => setContrasenaActual(e.target.value)}
                                        />
                                    </label>
                                )}
                                {campo('perfil-pais', 'País de Residencia', borrador.paisResidencia, 'paisResidencia', {
                                    autoComplete: 'country-name',
                                })}
                                <label className="perfil-campo" htmlFor="perfil-destinos">
                                    <span>Destinos Favoritos (separados por coma)</span>
                                    <input
                                        id="perfil-destinos"
                                        value={textoDestinos}
                                        onChange={(e) => setTextoDestinos(e.target.value)}
                                    />
                                </label>
                                <label className="perfil-campo" htmlFor="perfil-intereses">
                                    <span>Intereses (separados por coma)</span>
                                    <input
                                        id="perfil-intereses"
                                        value={textoIntereses}
                                        onChange={(e) => setTextoIntereses(e.target.value)}
                                    />
                                </label>
                            </>
                        )
                    }
                />
            </div>

            <h2 className="perfil__subtitulo">Algunas de tus opiniones:</h2>
            <ul className="perfil-opiniones">
                {opiniones.map((opinion) => (
                    <li key={opinion.id} className="perfil-opinion">
                        <blockquote>“{opinion.texto}”</blockquote>
                        <p>
                            <strong>{`${perfil.nombre} ${perfil.apellidos.split(' ')[0]}`}</strong>
                            <span>{perfil.nivelViajero}</span>
                        </p>
                    </li>
                ))}
            </ul>
        </div>
    );
};

export default Perfil;
