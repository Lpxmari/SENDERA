import { DificultadEnum } from './destino';

/** Actividad del catálogo (GET /actividades, docs/openapi.yaml). */
export interface Actividad {
    id: number;
    destinoId: number;
    destinoNombre: string;
    /** Ubicación del destino al que pertenece. */
    ubicacion: string;
    nombre: string;
    descripcion: string;
    duracionMinutos: number;
    dificultad: DificultadEnum;
    precioPorPersona: number;
    accesibilidad: boolean;
}

/** Una actividad colocada en un día y hora de un viaje guardado. */
export interface ViajeActividad {
    id: number;
    actividadId: number;
    destinoId: number;
    nombre: string;
    ubicacion: string;
    fecha: string; // YYYY-MM-DD
    horaInicio: string; // HH:mm (24 horas)
    duracionMinutos: number;
    dificultad: DificultadEnum;
    /** Posición dentro del día, desde 1. */
    orden: number;
}

export interface Viaje {
    id: number;
    nombre: string;
    fechaInicio: string;
    fechaFin: string;
    actividades: ViajeActividad[];
}

export interface ViajeResumen {
    id: number;
    nombre: string;
    ubicacion: string | null;
    fechaInicio: string;
    fechaFin: string;
    totalActividades: number;
    /** HH:mm de la primera actividad; nula si el viaje aún no tiene actividades. */
    primeraHora: string | null;
}

export interface ViajeActividadEntrada {
    actividadId: number;
    fecha: string;
    horaInicio: string;
}

/** Actividad tal como se edita en pantalla antes de guardar (la `clave` es solo local, para la lista). */
export interface ActividadEnViaje {
    clave: string;
    actividadId: number;
    nombre: string;
    ubicacion: string;
    duracionMinutos: number;
    dificultad: DificultadEnum;
    fecha: string;
    horaInicio: string;
}

/** Cuerpo de POST /viajes y PUT /viajes/{id}. El propietario lo define el servidor (JWT). */
export interface GuardarViajeRequest {
    nombre: string;
    fechaInicio: string;
    fechaFin: string;
    /** El orden de la lista define la posición de cada actividad dentro de su día. */
    actividades: ViajeActividadEntrada[];
}
