export interface Usuario {
    id: number;
    nombre: string;
    apellidos: string;
    correo: string;
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
