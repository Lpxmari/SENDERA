import {
    FiltroResenasAdmin,
    FiltroReservasAdmin,
    FiltroUsuariosAdmin,
    PaginaResenas,
    PaginaReservas,
    PaginaUsuarios,
    ReservaAdmin,
    RolUsuario,
    UsuarioAdmin,
} from '../types/admin';
import apiClient from './apiClient';

/** Todas las rutas /admin/** exigen ROL_ADMIN en el backend; los errores se propagan sin silenciarlos. */
export const listarUsuariosAdmin = async (filtros: FiltroUsuariosAdmin = {}): Promise<PaginaUsuarios> => {
    const params: Record<string, string | number | boolean> = {};
    if (filtros.q?.trim()) params.q = filtros.q.trim();
    if (filtros.rol) params.rol = filtros.rol;
    if (filtros.activo !== undefined && filtros.activo !== '') params.activo = filtros.activo;
    if (filtros.pagina !== undefined) params.pagina = filtros.pagina;
    if (filtros.tamano !== undefined) params.tamano = filtros.tamano;

    const { data } = await apiClient.get<PaginaUsuarios>('/admin/usuarios', { params });
    return data;
};

export const cambiarRolUsuario = async (id: number, rol: RolUsuario): Promise<UsuarioAdmin> => {
    const { data } = await apiClient.patch<UsuarioAdmin>(`/admin/usuarios/${id}/rol`, { rol });
    return data;
};

export const cambiarActivoUsuario = async (id: number, activo: boolean): Promise<UsuarioAdmin> => {
    const { data } = await apiClient.patch<UsuarioAdmin>(`/admin/usuarios/${id}/activo`, { activo });
    return data;
};

export const listarReservasAdmin = async (filtros: FiltroReservasAdmin = {}): Promise<PaginaReservas> => {
    const params: Record<string, string | number> = {};
    if (filtros.estado) params.estado = filtros.estado;
    if (filtros.q?.trim()) params.q = filtros.q.trim();
    if (filtros.desde) params.desde = filtros.desde;
    if (filtros.hasta) params.hasta = filtros.hasta;
    if (filtros.pagina !== undefined) params.pagina = filtros.pagina;
    if (filtros.tamano !== undefined) params.tamano = filtros.tamano;

    const { data } = await apiClient.get<PaginaReservas>('/admin/reservas', { params });
    return data;
};

/** Cancelar devuelve el cupo al itinerario; CANCELADA es un estado final (el backend responde 409). */
export const cambiarEstadoReserva = async (id: number, estado: 'CONFIRMADA' | 'CANCELADA'): Promise<ReservaAdmin> => {
    const { data } = await apiClient.patch<ReservaAdmin>(`/admin/reservas/${id}/estado`, { estado });
    return data;
};

export const listarResenasAdmin = async (filtros: FiltroResenasAdmin = {}): Promise<PaginaResenas> => {
    const params: Record<string, string | number> = {};
    if (filtros.destinoId) params.destinoId = filtros.destinoId;
    if (filtros.calificacionMax) params.calificacionMax = filtros.calificacionMax;
    if (filtros.q?.trim()) params.q = filtros.q.trim();
    if (filtros.pagina !== undefined) params.pagina = filtros.pagina;
    if (filtros.tamano !== undefined) params.tamano = filtros.tamano;

    const { data } = await apiClient.get<PaginaResenas>('/admin/resenas', { params });
    return data;
};

export const eliminarResenaAdmin = async (id: number): Promise<void> => {
    await apiClient.delete(`/admin/resenas/${id}`);
};
