import { InputHTMLAttributes, useId, useState } from 'react';

interface Props extends Omit<InputHTMLAttributes<HTMLInputElement>, 'onChange'> {
    etiqueta: string;
    valor: string;
    onCambio: (valor: string) => void;
    error?: string | null;
    ayuda?: string;
}

const IconoOjo = ({ tachado }: { tachado: boolean }) => (
    <svg
        viewBox="0 0 24 24"
        width="18"
        height="18"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
        strokeLinejoin="round"
        aria-hidden="true"
    >
        <path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7S2 12 2 12Z" />
        <circle cx="12" cy="12" r="3" />
        {tachado && <path d="m4 4 16 16" />}
    </svg>
);

/** Campo con etiqueta, mensaje de error y (si type="password") botón para ver/ocultar. */
const CampoTexto = ({ etiqueta, valor, onCambio, error, ayuda, type = 'text', ...resto }: Props) => {
    const id = useId();
    const [visible, setVisible] = useState(false);
    const esContrasena = type === 'password';

    return (
        <div className="auth-campo">
            <label htmlFor={id} className="auth-campo__etiqueta">
                {etiqueta}
            </label>
            <div className="auth-campo__control">
                <input
                    id={id}
                    type={esContrasena && visible ? 'text' : type}
                    value={valor}
                    onChange={(e) => onCambio(e.target.value)}
                    aria-invalid={error ? true : undefined}
                    aria-describedby={error ? `${id}-error` : ayuda ? `${id}-ayuda` : undefined}
                    className={error ? 'auth-campo__input auth-campo__input--error' : 'auth-campo__input'}
                    {...resto}
                />
                {esContrasena && (
                    <button
                        type="button"
                        className="auth-campo__ojo"
                        onClick={() => setVisible((v) => !v)}
                        aria-label={visible ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                    >
                        <IconoOjo tachado={visible} />
                    </button>
                )}
            </div>
            {error && (
                <p id={`${id}-error`} className="auth-campo__error" role="alert">
                    {error}
                </p>
            )}
            {!error && ayuda && (
                <p id={`${id}-ayuda`} className="auth-campo__ayuda">
                    {ayuda}
                </p>
            )}
        </div>
    );
};

export default CampoTexto;
