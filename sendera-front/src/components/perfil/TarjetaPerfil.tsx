import { FormEvent, ReactNode } from 'react';

interface TarjetaPerfilProps {
    id: string;
    titulo: string;
    editando: boolean;
    error: string | null;
    onEditar: () => void;
    onCancelar: () => void;
    onGuardar: () => void;
    /** Contenido de solo lectura. */
    vista: ReactNode;
    /** Campos del formulario de edición. */
    formulario: ReactNode;
}

/** Tarjeta del perfil con modo de lectura y modo de edición independientes. */
const TarjetaPerfil = ({
    id,
    titulo,
    editando,
    error,
    onEditar,
    onCancelar,
    onGuardar,
    vista,
    formulario,
}: TarjetaPerfilProps) => {
    const handleSubmit = (evento: FormEvent<HTMLFormElement>) => {
        evento.preventDefault();
        onGuardar();
    };

    return (
        <section className="perfil-tarjeta" aria-labelledby={`${id}-titulo`}>
            <header className="perfil-tarjeta__cabecera">
                <h2 id={`${id}-titulo`}>{titulo}</h2>
                {!editando && (
                    <button
                        type="button"
                        className="perfil-boton-editar"
                        onClick={onEditar}
                        aria-label={`Editar ${titulo.toLowerCase()}`}
                    >
                        Editar
                    </button>
                )}
            </header>

            {editando ? (
                <form className="perfil-formulario" onSubmit={handleSubmit} noValidate>
                    {formulario}
                    {error && (
                        <p className="perfil-error" role="alert">
                            {error}
                        </p>
                    )}
                    <div className="perfil-formulario__acciones">
                        <button type="button" className="boton-contorno" onClick={onCancelar}>
                            Cancelar
                        </button>
                        <button type="submit" className="boton boton--verde">
                            Guardar cambios
                        </button>
                    </div>
                </form>
            ) : (
                <dl className="perfil-lista">{vista}</dl>
            )}
        </section>
    );
};

export default TarjetaPerfil;
