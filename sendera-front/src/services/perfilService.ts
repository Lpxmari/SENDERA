import { AuthResponse, Usuario } from '../types/auth';
import { OpinionViajero, PerfilEditable, PerfilViajero, ViajeHistorial } from '../types/perfil';
import { historialSimulado, opinionesSimuladas, perfilSimulado } from '../utils/datosPerfil';
import { actualizarCuenta } from './authService';

/*
 * Nombre, apellidos y correo son REALES: se leen de la sesión y se guardan con PUT /auth/me.
 * El resto (identificación, fecha de nacimiento, país, destinos favoritos, intereses, foto, opiniones e historial)
 * sigue SIMULADO en memoria hasta que el MER y docs/openapi.yaml definan esos campos; se pierde al recargar.
 */
const perfilesGuardados = new Map<number, PerfilViajero>();

/** Mezcla los datos reales de la cuenta (sesión) con los campos simulados de esa persona. */
export const fetchPerfil = async (usuario: Usuario): Promise<PerfilViajero> => {
    const real = { nombre: usuario.nombre, apellidos: usuario.apellidos, correo: usuario.correo };
    const guardado = perfilesGuardados.get(usuario.id);
    const perfil = guardado ? { ...guardado, ...real } : perfilSimulado(real);
    perfilesGuardados.set(usuario.id, perfil);
    return perfil;
};

export interface ResultadoActualizarPerfil {
    perfil: PerfilViajero;
    /** Sesión nueva devuelta por el backend; quien llama debe guardarla (el token anterior puede dejar de servir). */
    sesion: AuthResponse;
}

/**
 * Guarda en el backend nombre, apellidos y correo, y en memoria el resto. Si el correo cambia, el backend exige
 * `contrasenaActual`. Los errores se propagan sin silenciarlos para que la pantalla muestre el mensaje del servidor.
 */
export const actualizarPerfil = async (
    usuarioId: number,
    cambios: PerfilEditable,
    contrasenaActual?: string,
): Promise<ResultadoActualizarPerfil> => {
    const actual = perfilesGuardados.get(usuarioId);
    if (actual === undefined) throw new Error('El perfil no se ha cargado.');

    const sesion = await actualizarCuenta({
        nombre: cambios.nombre,
        apellidos: cambios.apellidos,
        correo: cambios.correo,
        contrasenaActual: contrasenaActual || undefined,
    });

    const perfil = {
        ...actual,
        ...cambios,
        nombre: sesion.usuario.nombre,
        apellidos: sesion.usuario.apellidos,
        correo: sesion.usuario.correo,
    };
    perfilesGuardados.set(usuarioId, perfil);
    return { perfil, sesion };
};

export const fetchOpiniones = async (): Promise<OpinionViajero[]> => opinionesSimuladas;

export const fetchHistorialViajes = async (): Promise<ViajeHistorial[]> => historialSimulado;

/** Solo para pruebas: reinicia el perfil simulado. */
export const reiniciarPerfilSimulado = (): void => {
    perfilesGuardados.clear();
};
