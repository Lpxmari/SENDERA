import { CrearDestinoDto, Destino, FiltroDestinoParams, PaginaDestinos } from '../types/destino';
import { ItinerarioDisponible } from '../types/itinerario';
import apiClient from './apiClient';

/** Los errores se propagan sin silenciarlos; la capa de UI decide cómo mostrarlos (ver obtenerMensajeError). */
export const listarDestinos = async (filtros?: FiltroDestinoParams): Promise<PaginaDestinos> => {
    const params: Record<string, string | number | boolean> = {};
    if (filtros?.q) params.q = filtros.q;
    if (filtros?.nombre) params.nombre = filtros.nombre;
    if (filtros?.ubicacion) params.ubicacion = filtros.ubicacion;
    if (filtros?.page !== undefined) params.page = filtros.page;
    if (filtros?.size !== undefined) params.size = filtros.size;
    if (filtros?.precioMin !== undefined) params.precioMin = filtros.precioMin;
    if (filtros?.precioMax !== undefined) params.precioMax = filtros.precioMax;
    if (filtros?.dificultad) params.dificultad = filtros.dificultad;
    if (filtros?.accesibilidad !== undefined && filtros.accesibilidad !== null) {
        params.accesibilidad = filtros.accesibilidad;
    }

    const { data } = await apiClient.get<PaginaDestinos>('/destinos', { params });
    return data;
};

const TAMANO_MAXIMO_PAGINA = 50;

/**
 * Recorre todas las páginas (de 50 en 50). Solo para listas desplegables del panel de administración que necesitan
 * todos los destinos; las pantallas públicas deben pedir una página.
 */
export const listarTodosLosDestinos = async (): Promise<Destino[]> => {
    const destinos: Destino[] = [];
    let pagina = 0;
    let totalPaginas = 1;
    while (pagina < totalPaginas) {
        const respuesta = await listarDestinos({ page: pagina, size: TAMANO_MAXIMO_PAGINA });
        destinos.push(...respuesta.contenido);
        totalPaginas = respuesta.totalPaginas;
        pagina += 1;
    }
    return destinos;
};

export const obtenerDestinoPorId = async (id: number): Promise<Destino> => {
    const { data } = await apiClient.get<Destino>(`/destinos/${id}`);
    return data;
};

/** Itinerarios reservables (con cupos y que no han terminado); su id es el itinerarioId de POST /reservas. */
export const listarItinerariosDeDestino = async (destinoId: number): Promise<ItinerarioDisponible[]> => {
    const { data } = await apiClient.get<ItinerarioDisponible[]>(`/destinos/${destinoId}/itinerarios`);
    return data;
};

/** Solo administradores (el backend responde 403 a cualquier otro rol). */
export const crearDestino = async (datos: CrearDestinoDto): Promise<Destino> => {
    const { data } = await apiClient.post<Destino>('/destinos', datos);
    return data;
};

export const actualizarDestino = async (id: number, datos: CrearDestinoDto): Promise<Destino> => {
    const { data } = await apiClient.put<Destino>(`/destinos/${id}`, datos);
    return data;
};

/** 409 si el destino tiene itinerarios asociados. */
export const eliminarDestino = async (id: number): Promise<void> => {
    await apiClient.delete(`/destinos/${id}`);
};

// Alias para mantener compatibilidad con los componentes que usan los nombres anteriores
export const fetchDestinos = listarDestinos;
export const getDestinoPorId = obtenerDestinoPorId;
