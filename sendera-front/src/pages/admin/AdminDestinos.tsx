import { FormEvent, useCallback, useEffect, useState } from 'react';
import DialogoConfirmacion from '../../components/admin/DialogoConfirmacion';
import BotonIcono from '../../components/BotonIcono';
import IconoAdmin from '../../components/admin/IconoAdmin';
import { obtenerMensajeError } from '../../services/apiClient';
import { actualizarDestino, crearDestino, eliminarDestino, listarDestinos } from '../../services/destinoService';
import { CrearDestinoDto, DificultadEnum, Destino } from '../../types/destino';
import { formatearPrecio } from '../../utils/formatearPrecio';

interface BorradorDestino {
    nombre: string;
    descripcion: string;
    ubicacion: string;
    precioBase: string;
    dificultad: DificultadEnum;
    accesibilidad: boolean;
    fotos: string; // una URL o ruta por línea
}

const BORRADOR_VACIO: BorradorDestino = {
    nombre: '',
    descripcion: '',
    ubicacion: '',
    precioBase: '',
    dificultad: 'BAJA',
    accesibilidad: false,
    fotos: '',
};

const MAX_TEXTO = 255;

const desdeDestino = (d: Destino): BorradorDestino => ({
    nombre: d.nombre,
    descripcion: d.descripcion,
    ubicacion: d.ubicacion,
    precioBase: String(d.precioBase),
    dificultad: d.dificultad,
    accesibilidad: d.accesibilidad,
    fotos: (d.fotosUrl ?? []).join('\n'),
});

const listaFotos = (texto: string): string[] =>
    texto
        .split('\n')
        .map((linea) => linea.trim())
        .filter(Boolean);

/** Valida lo mismo que el backend para avisar antes de enviar; el backend sigue validando siempre. */
const validar = (b: BorradorDestino): string | null => {
    if (!b.nombre.trim()) return 'Escribe el nombre del destino.';
    if (b.nombre.trim().length > MAX_TEXTO) return `El nombre no puede superar los ${MAX_TEXTO} caracteres.`;
    if (!b.descripcion.trim()) return 'Escribe una descripción del destino.';
    if (!b.ubicacion.trim()) return 'Escribe la ubicación del destino.';
    if (b.ubicacion.trim().length > MAX_TEXTO) return `La ubicación no puede superar los ${MAX_TEXTO} caracteres.`;
    const precio = Number(b.precioBase);
    if (b.precioBase.trim() === '' || Number.isNaN(precio)) return 'Escribe el precio base como un número.';
    if (precio < 0) return 'El precio base no puede ser negativo.';
    if (listaFotos(b.fotos).some((f) => f.length > MAX_TEXTO)) return `Cada foto admite hasta ${MAX_TEXTO} caracteres.`;
    return null;
};

const aDto = (b: BorradorDestino): CrearDestinoDto => ({
    nombre: b.nombre.trim(),
    descripcion: b.descripcion.trim(),
    ubicacion: b.ubicacion.trim(),
    precioBase: Number(b.precioBase),
    dificultad: b.dificultad,
    accesibilidad: b.accesibilidad,
    fotosUrl: listaFotos(b.fotos),
});

const AdminDestinos = () => {
    const [destinos, setDestinos] = useState<Destino[] | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [aviso, setAviso] = useState<string | null>(null);
    const [editando, setEditando] = useState<Destino | 'nuevo' | null>(null);
    const [borrador, setBorrador] = useState<BorradorDestino>(BORRADOR_VACIO);
    const [errorFormulario, setErrorFormulario] = useState<string | null>(null);
    const [guardando, setGuardando] = useState(false);
    const [porEliminar, setPorEliminar] = useState<Destino | null>(null);
    const [eliminando, setEliminando] = useState(false);
    const [recarga, setRecarga] = useState(0);

    useEffect(() => {
        let activo = true;
        listarDestinos()
            .then((lista) => {
                if (!activo) return;
                setDestinos(lista);
                setError(null);
            })
            .catch((err: unknown) => {
                if (activo) setError(obtenerMensajeError(err, 'No pudimos cargar los destinos. Intenta de nuevo.'));
            });
        return () => {
            activo = false;
        };
    }, [recarga]);

    const abrirNuevo = () => {
        setBorrador(BORRADOR_VACIO);
        setErrorFormulario(null);
        setAviso(null);
        setEditando('nuevo');
    };

    const abrirEdicion = (destino: Destino) => {
        setBorrador(desdeDestino(destino));
        setErrorFormulario(null);
        setAviso(null);
        setEditando(destino);
    };

    const cerrarFormulario = () => {
        setEditando(null);
        setErrorFormulario(null);
    };

    const cambiar = <K extends keyof BorradorDestino>(campo: K, valor: BorradorDestino[K]) =>
        setBorrador((actual) => ({ ...actual, [campo]: valor }));

    const handleGuardar = async (evento: FormEvent<HTMLFormElement>) => {
        evento.preventDefault();
        const mensaje = validar(borrador);
        if (mensaje) {
            setErrorFormulario(mensaje);
            return;
        }
        setGuardando(true);
        try {
            if (editando === 'nuevo') {
                await crearDestino(aDto(borrador));
                setAviso('El destino se creó correctamente.');
            } else if (editando) {
                await actualizarDestino(editando.id, aDto(borrador));
                setAviso('Los cambios del destino se guardaron.');
            }
            cerrarFormulario();
            setRecarga((n) => n + 1);
        } catch (err) {
            setErrorFormulario(obtenerMensajeError(err, 'No pudimos guardar el destino. Intenta de nuevo.'));
        } finally {
            setGuardando(false);
        }
    };

    const cancelarEliminar = useCallback(() => setPorEliminar(null), []);

    const confirmarEliminar = async () => {
        if (!porEliminar) return;
        setEliminando(true);
        try {
            await eliminarDestino(porEliminar.id);
            setAviso(`Se eliminó el destino “${porEliminar.nombre}”.`);
            setError(null);
            setRecarga((n) => n + 1);
        } catch (err) {
            setAviso(null);
            setError(obtenerMensajeError(err, 'No pudimos eliminar el destino. Intenta de nuevo.'));
        } finally {
            setEliminando(false);
            setPorEliminar(null);
        }
    };

    return (
        <section aria-labelledby="admin-destinos-titulo">
            <div className="admin__encabezado">
                <h1 id="admin-destinos-titulo" className="admin__h1">
                    Destinos
                </h1>
                {!editando && (
                    <button type="button" className="boton boton--verde boton--con-icono" onClick={abrirNuevo}>
                        <IconoAdmin nombre="mas" />
                        Nuevo destino
                    </button>
                )}
            </div>
            <p className="admin__ayuda">Crea, edita o elimina los destinos que ven los viajeros.</p>

            {aviso && (
                <p className="admin-aviso" role="status">
                    {aviso}
                </p>
            )}
            {error && (
                <p className="admin-error" role="alert">
                    {error}
                </p>
            )}

            {editando && (
                <form
                    className="admin-formulario"
                    onSubmit={handleGuardar}
                    noValidate
                    aria-labelledby="admin-destino-formulario"
                >
                    <h2 id="admin-destino-formulario">
                        {editando === 'nuevo' ? 'Nuevo destino' : `Editar “${editando.nombre}”`}
                    </h2>

                    <label className="admin-campo" htmlFor="destino-nombre">
                        <span>Nombre</span>
                        <input
                            id="destino-nombre"
                            value={borrador.nombre}
                            onChange={(e) => cambiar('nombre', e.target.value)}
                        />
                    </label>
                    <label className="admin-campo" htmlFor="destino-ubicacion">
                        <span>Ubicación</span>
                        <input
                            id="destino-ubicacion"
                            value={borrador.ubicacion}
                            onChange={(e) => cambiar('ubicacion', e.target.value)}
                        />
                    </label>
                    <label className="admin-campo" htmlFor="destino-descripcion">
                        <span>Descripción</span>
                        <textarea
                            id="destino-descripcion"
                            rows={4}
                            value={borrador.descripcion}
                            onChange={(e) => cambiar('descripcion', e.target.value)}
                        />
                    </label>
                    <div className="admin-formulario__fila">
                        <label className="admin-campo" htmlFor="destino-precio">
                            <span>Precio base (COP)</span>
                            <input
                                id="destino-precio"
                                inputMode="decimal"
                                value={borrador.precioBase}
                                onChange={(e) => cambiar('precioBase', e.target.value)}
                            />
                        </label>
                        <label className="admin-campo" htmlFor="destino-dificultad">
                            <span>Dificultad</span>
                            <select
                                id="destino-dificultad"
                                value={borrador.dificultad}
                                onChange={(e) => cambiar('dificultad', e.target.value as DificultadEnum)}
                            >
                                <option value="BAJA">Baja</option>
                                <option value="MODERADA">Moderada</option>
                                <option value="ALTA">Alta</option>
                            </select>
                        </label>
                    </div>
                    <label className="admin-casilla" htmlFor="destino-accesibilidad">
                        <input
                            id="destino-accesibilidad"
                            type="checkbox"
                            checked={borrador.accesibilidad}
                            onChange={(e) => cambiar('accesibilidad', e.target.checked)}
                        />
                        <span>Es accesible para personas con movilidad reducida</span>
                    </label>
                    <div className="admin-campo">
                        <label htmlFor="destino-fotos">Fotos (una dirección por línea)</label>
                        <textarea
                            id="destino-fotos"
                            rows={3}
                            value={borrador.fotos}
                            onChange={(e) => cambiar('fotos', e.target.value)}
                            aria-describedby="destino-fotos-ayuda"
                        />
                        <small id="destino-fotos-ayuda">
                            Usa direcciones completas o rutas del sitio, por ejemplo
                            /imagenes/alojamientos/casa-aurora-1.jpg. La primera foto es la portada.
                        </small>
                    </div>

                    {errorFormulario && (
                        <p className="admin-error" role="alert">
                            {errorFormulario}
                        </p>
                    )}
                    <div className="admin-formulario__acciones">
                        <button
                            type="button"
                            className="boton-contorno"
                            onClick={cerrarFormulario}
                            disabled={guardando}
                        >
                            Cancelar
                        </button>
                        <button type="submit" className="boton boton--verde" disabled={guardando}>
                            {guardando ? 'Guardando…' : 'Guardar destino'}
                        </button>
                    </div>
                </form>
            )}

            {destinos === null && !error && <p role="status">Cargando destinos…</p>}
            {destinos && destinos.length === 0 && (
                <p className="admin-vacio">Todavía no hay destinos. Crea el primero.</p>
            )}

            {destinos && destinos.length > 0 && (
                <div className="admin-tabla__contenedor">
                    <table className="admin-tabla">
                        <caption className="sr-only">Lista de destinos</caption>
                        <thead>
                            <tr>
                                <th scope="col">Nombre</th>
                                <th scope="col">Ubicación</th>
                                <th scope="col">Precio base</th>
                                <th scope="col">Dificultad</th>
                                <th scope="col">Accesible</th>
                                <th scope="col">Acciones</th>
                            </tr>
                        </thead>
                        <tbody>
                            {destinos.map((d) => (
                                <tr key={d.id}>
                                    <td>{d.nombre}</td>
                                    <td>{d.ubicacion}</td>
                                    <td>{formatearPrecio(d.precioBase)}</td>
                                    <td>{d.dificultad.charAt(0) + d.dificultad.slice(1).toLowerCase()}</td>
                                    <td>{d.accesibilidad ? 'Sí' : 'No'}</td>
                                    <td>
                                        <div className="admin-tabla__acciones">
                                            <BotonIcono
                                                icono="lapiz"
                                                etiqueta={`Editar ${d.nombre}`}
                                                ayuda="Editar"
                                                onClick={() => abrirEdicion(d)}
                                            />
                                            <BotonIcono
                                                icono="papelera"
                                                etiqueta={`Eliminar ${d.nombre}`}
                                                ayuda="Eliminar"
                                                peligro
                                                onClick={() => setPorEliminar(d)}
                                            />
                                        </div>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}

            {porEliminar && (
                <DialogoConfirmacion
                    peligro
                    titulo="Eliminar destino"
                    mensaje={`¿Seguro que quieres eliminar “${porEliminar.nombre}”? Esta acción no se puede deshacer. Si tiene itinerarios, no se podrá eliminar.`}
                    textoConfirmar="Sí, eliminar"
                    procesando={eliminando}
                    onConfirmar={confirmarEliminar}
                    onCancelar={cancelarEliminar}
                />
            )}
        </section>
    );
};

export default AdminDestinos;
