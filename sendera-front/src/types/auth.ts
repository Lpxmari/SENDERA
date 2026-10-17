export type RolUsuario = 'ROLE_ADMIN' | 'ROLE_CLIENTE';

export interface Usuario {
    id: number;
    nombre: string;
    apellidos: string;
    correo: string;
    /** Solo para mostrar u ocultar menús: el backend autoriza siempre con el rol firmado en el JWT. */
    rol?: RolUsuario;
}

export interface LoginRequest {
    correo: string;
    contrasena: string;
}

export interface RegistroRequest {
    nombre: string;
    apellidos: string;
    correo: string;
    contrasena: string;
}

/** Cuerpo de PUT /auth/me. `contrasenaActual` es obligatoria solo si el correo cambia. */
export interface ActualizarCuentaRequest {
    nombre: string;
    apellidos: string;
    correo: string;
    contrasenaActual?: string;
}

export interface AuthResponse {
    token: string;
    usuario: Usuario;
}

export interface RecuperarContrasenaRequest {
    correo: string;
}

export interface CambiarContrasenaRequest {
    token: string;
    nuevaContrasena: string;
}
