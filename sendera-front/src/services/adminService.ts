import { FiltroUsuariosAdmin, PaginaUsuarios, RolUsuario, UsuarioAdmin } from '../types/admin';
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
