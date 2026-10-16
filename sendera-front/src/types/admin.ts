import { RolUsuario } from './auth';

export type { RolUsuario };

/** Usuario tal como lo ve un administrador (GET /admin/usuarios). */
export interface UsuarioAdmin {
    id: number;
    nombre: string;
    apellidos: string;
    correo: string;
    rol: RolUsuario;
    activo: boolean;
    fechaRegistro: string; // ISO date-time
}

export interface PaginaUsuarios {
    contenido: UsuarioAdmin[];
    pagina: number; // empieza en 0
    tamano: number;
    total: number;
}

export interface FiltroUsuariosAdmin {
    q?: string;
    rol?: RolUsuario | '';
    activo?: boolean | '';
    pagina?: number;
    tamano?: number;
}
