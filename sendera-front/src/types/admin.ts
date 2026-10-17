import { RolUsuario } from './auth';
import { EstadoReserva } from './reserva';

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

/** Reserva tal como la ve un administrador (GET /admin/reservas). */
export interface ReservaAdmin {
    id: number;
    usuarioId: number;
    usuarioNombre: string;
    usuarioCorreo: string;
    itinerarioId: number;
    itinerarioNombre: string;
    destinoNombre: string;
    fechaReserva: string; // YYYY-MM-DD
    montoTotal: number;
    estadoReserva: EstadoReserva;
}

export interface PaginaReservas {
    contenido: ReservaAdmin[];
    pagina: number;
    tamano: number;
    total: number;
}

export interface FiltroReservasAdmin {
    estado?: EstadoReserva | '';
    q?: string;
    desde?: string;
    hasta?: string;
    pagina?: number;
    tamano?: number;
}

/** Reseña vista por un administrador para moderarla (GET /admin/resenas). */
export interface ResenaAdmin {
    id: number;
    destinoId: number;
    destinoNombre: string;
    usuarioId: number;
    autor: string;
    comentario: string;
    calificacion: number; // 1 a 5
    tieneImagen: boolean;
    creadaEn: string; // ISO date-time
}

export interface PaginaResenas {
    contenido: ResenaAdmin[];
    pagina: number;
    tamano: number;
    total: number;
}

export interface FiltroResenasAdmin {
    destinoId?: number | '';
    calificacionMax?: number | '';
    q?: string;
    pagina?: number;
    tamano?: number;
}
