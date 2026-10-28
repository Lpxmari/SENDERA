import { DificultadEnum } from '../types/destino';

/**
 * Formato y reglas de horario de los itinerarios. El backend repite las reglas (nunca se confía solo en la
 * interfaz): aquí sirven para avisar a la persona antes de guardar.
 */

const MESES = [
    'enero',
    'febrero',
    'marzo',
    'abril',
    'mayo',
    'junio',
    'julio',
    'agosto',
    'septiembre',
    'octubre',
    'noviembre',
    'diciembre',
];

const MAX_DIAS_VISIBLES = 400;

const partes = (iso: string): [number, number, number] => {
    const [anio, mes, dia] = iso.split('-').map(Number);
    return [anio, mes, dia];
};

/** Fecha de hoy (YYYY-MM-DD) en Colombia, igual que el backend: así la validación no cambia según la zona del navegador. */
export const hoyIso = (): string => new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Bogota' }).format(new Date());

/** "2026-10-15" -> "15 de octubre" (con el año si no es el actual). */
export const formatearDia = (iso: string): string => {
    const [anio, mes, dia] = partes(iso);
    const sufijo = anio !== partes(hoyIso())[0] ? ` de ${anio}` : '';
    return `${dia} de ${MESES[mes - 1]}${sufijo}`;
};

const DIAS_SEMANA = ['Domingo', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'];

/** "2026-10-15" -> "Jueves 15 de octubre". */
export const formatearDiaConSemana = (iso: string): string => {
    const [anio, mes, dia] = partes(iso);
    return `${DIAS_SEMANA[new Date(Date.UTC(anio, mes - 1, dia)).getUTCDay()]} ${formatearDia(iso)}`;
};

/** Mismo formato en todas las pantallas: "15 - 18 de octubre","28 de octubre - 2 de noviembre", "15 de octubre de 2027". */
export const formatearRangoFechas = (inicio: string, fin: string): string => {
    if (inicio === fin) return formatearDia(inicio);
    const [anio1, mes1, dia1] = partes(inicio);
    const [anio2, mes2, dia2] = partes(fin);
    const anioActual = partes(hoyIso())[0];
    if (anio1 !== anio2) return `${dia1} de ${MESES[mes1 - 1]} de ${anio1} - ${dia2} de ${MESES[mes2 - 1]} de ${anio2}`;
    const sufijo = anio1 !== anioActual ? ` de ${anio1}` : '';
    if (mes1 === mes2) return `${dia1} - ${dia2} de ${MESES[mes1 - 1]}${sufijo}`;
    return `${dia1} de ${MESES[mes1 - 1]} - ${dia2} de ${MESES[mes2 - 1]}${sufijo}`;
};

/** Todos los días del viaje, ambos incluidos; vacío si las fechas no son válidas. */
export const diasDelViaje = (inicio: string, fin: string): string[] => {
    if (!inicio || !fin || fin < inicio) return [];
    const [anio, mes, dia] = partes(inicio);
    const dias: string[] = [];
    for (let i = 0; i < MAX_DIAS_VISIBLES; i++) {
        const actual = new Date(Date.UTC(anio, mes - 1, dia + i)).toISOString().slice(0, 10);
        if (actual > fin) break;
        dias.push(actual);
    }
    return dias;
};

export const minutosDelDia = (hora: string): number => {
    const [h, m] = hora.split(':').map(Number);
    return h * 60 + m;
};

/** Copia ordenada de más temprana a más tarde; a igual hora conserva el orden original. */
export const ordenarPorHora = <T extends { horaInicio: string }>(actividades: T[]): T[] =>
    [...actividades].sort((a, b) => minutosDelDia(a.horaInicio) - minutosDelDia(b.horaInicio));

/** "14:00" -> "2:00 p. m." (mismo formato en toda la aplicación). */
export const formatearHora = (hora: string): string => {
    const [h, m] = hora.split(':').map(Number);
    const sufijo = h >= 12 ? 'p. m.' : 'a. m.';
    const hora12 = h % 12 === 0 ? 12 : h % 12;
    return `${hora12}:${String(m).padStart(2, '0')} ${sufijo}`;
};

/** Hora en la que termina una actividad que empieza a `hora`; se acota a 23:59 para no cruzar de día. */
export const horaFin = (hora: string, duracionMinutos: number): string => {
    const total = Math.min(minutosDelDia(hora) + duracionMinutos, 24 * 60 - 1);
    return `${String(Math.floor(total / 60)).padStart(2, '0')}:${String(total % 60).padStart(2, '0')}`;
};

/** 120 -> "2 horas", 60 -> "1 hora", 90 -> "1 h 30 min", 45 -> "45 min". */
export const formatearDuracion = (minutos: number): string => {
    const horas = Math.floor(minutos / 60);
    const resto = minutos % 60;
    if (horas === 0) return `${resto} min`;
    if (resto === 0) return horas === 1 ? '1 hora' : `${horas} horas`;
    return `${horas} h ${resto} min`;
};

export const etiquetaDificultad = (dificultad: DificultadEnum): string => {
    const textos: Record<DificultadEnum, string> = { BAJA: 'baja', MODERADA: 'moderada', ALTA: 'alta' };
    return `Dificultad ${textos[dificultad]}`;
};

export interface ActividadHorario {
    clave: string;
    nombre: string;
    fecha: string;
    horaInicio: string;
    duracionMinutos: number;
}

export interface Solape {
    fecha: string;
    primera: ActividadHorario;
    segunda: ActividadHorario;
}

const rango = (a: ActividadHorario): string =>
    `«${a.nombre}» (${formatearHora(a.horaInicio)} – ${formatearHora(horaFin(a.horaInicio, a.duracionMinutos))})`;

export const describirSolape = (solape: Solape): string =>
    `${rango(solape.primera)} y ${rango(solape.segunda)} se solapan el ${formatearDia(solape.fecha)}.`;

/**
 * Pares de actividades del mismo día que se pisan. Una termina justo cuando empieza otra: no es solape.
 * Misma regla que el backend: se compara cada actividad con la que más tarde termina de las anteriores.
 */
export const detectarSolapes = (actividades: ActividadHorario[]): Solape[] => {
    const porDia = new Map<string, ActividadHorario[]>();
    for (const actividad of actividades) {
        porDia.set(actividad.fecha, [...(porDia.get(actividad.fecha) ?? []), actividad]);
    }
    const solapes: Solape[] = [];
    for (const [fecha, delDia] of [...porDia.entries()].sort(([a], [b]) => a.localeCompare(b))) {
        const ordenadas = [...delDia].sort((a, b) => minutosDelDia(a.horaInicio) - minutosDelDia(b.horaInicio));
        let anterior: ActividadHorario | null = null;
        let finAnterior = 0;
        for (const actual of ordenadas) {
            const inicio = minutosDelDia(actual.horaInicio);
            if (anterior && inicio < finAnterior) solapes.push({ fecha, primera: anterior, segunda: actual });
            const fin = inicio + actual.duracionMinutos;
            if (!anterior || fin > finAnterior) {
                anterior = actual;
                finAnterior = fin;
            }
        }
    }
    return solapes;
};
