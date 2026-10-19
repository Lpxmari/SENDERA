export const CORREO_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
export const MIN_CONTRASENA = 8;

export const validarCorreo = (correo: string): string | null => {
    if (!correo.trim()) return 'Ingresa tu correo electrónico.';
    if (!CORREO_REGEX.test(correo.trim())) return 'Ingresa un correo válido, por ejemplo: maria@correo.com';
    return null;
};

export const validarContrasenaNueva = (contrasena: string): string | null => {
    if (!contrasena) return 'Ingresa una contraseña.';
    if (contrasena.length < MIN_CONTRASENA) return `La contraseña debe tener al menos ${MIN_CONTRASENA} caracteres.`;
    if (!/[A-Za-z]/.test(contrasena) || !/\d/.test(contrasena)) return 'Usa letras y números en tu contraseña.';
    return null;
};

export const validarCoincidencia = (a: string, b: string): string | null =>
    a !== b ? 'Las contraseñas no coinciden.' : null;

export const validarCodigo = (codigo: string): string | null => {
    if (!codigo.trim()) return 'Ingresa el código que te enviamos.';
    if (!/^\d{6}$/.test(codigo.trim())) return 'El código tiene 6 números.';
    return null;
};
