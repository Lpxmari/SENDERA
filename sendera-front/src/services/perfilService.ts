import { OpinionViajero, PerfilEditable, PerfilViajero, ViajeHistorial } from '../types/perfil';
import { Usuario } from '../types/auth';
import { historialSimulado, opinionesSimuladas, perfilSimulado } from '../utils/datosPerfil';

/*
 * SIMULADO: todavía no hay endpoints de perfil en docs/openapi.yaml.
 * Al conectarlo, solo cambia el cuerpo de estas funciones por llamadas a apiClient
 * (p. ej. GET/PUT /usuarios/me); las firmas y los tipos se mantienen.
 */
/** Perfiles simulados por id de usuario: cada sesión ve solo el suyo. */
const perfilesGuardados = new Map<number, PerfilViajero>();

export const fetchPerfil = async (usuario: Usuario): Promise<PerfilViajero> => {
    let perfil = perfilesGuardados.get(usuario.id);
    if (perfil === undefined) {
        perfil = perfilSimulado({
            nombre: usuario.nombre,
            apellidos: usuario.apellidos,
            correo: usuario.correo,
        });
        perfilesGuardados.set(usuario.id, perfil);
    }
    return perfil;
};

export const actualizarPerfil = async (usuarioId: number, cambios: PerfilEditable): Promise<PerfilViajero> => {
    const actual = perfilesGuardados.get(usuarioId);
    if (actual === undefined) throw new Error('El perfil no se ha cargado.');
    const actualizado = { ...actual, ...cambios };
    perfilesGuardados.set(usuarioId, actualizado);
    return actualizado;
};

export const fetchOpiniones = async (): Promise<OpinionViajero[]> => opinionesSimuladas;

export const fetchHistorialViajes = async (): Promise<ViajeHistorial[]> => historialSimulado;

/** Solo para pruebas: reinicia el perfil simulado. */
export const reiniciarPerfilSimulado = (): void => {
    perfilesGuardados.clear();
};
