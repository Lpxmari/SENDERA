import { describe, expect, it } from 'vitest';
import {
    ActividadHorario,
    describirSolape,
    detectarSolapes,
    diasDelViaje,
    etiquetaDificultad,
    formatearDia,
    formatearDiaConSemana,
    formatearDuracion,
    formatearHora,
    formatearRangoFechas,
    horaFin,
    hoyIso,
    ordenarPorHora,
} from './itinerario';

const anio = hoyIso().slice(0, 4);

const actividad = (clave: string, fecha: string, horaInicio: string, duracionMinutos: number): ActividadHorario => ({
    clave,
    nombre: `Actividad ${clave}`,
    fecha,
    horaInicio,
    duracionMinutos,
});

describe('formato de fechas', () => {
    it('hoyIso devuelve una fecha ISO', () => {
        expect(hoyIso()).toMatch(/^\d{4}-\d{2}-\d{2}$/);
    });

    it('formatea un día, con el año solo si no es el actual', () => {
        expect(formatearDia(`${anio}-10-15`)).toBe('15 de octubre');
        expect(formatearDia('2099-01-05')).toBe('5 de enero de 2099');
    });

    it('usa el mismo formato para todos los rangos', () => {
        expect(formatearRangoFechas(`${anio}-10-15`, `${anio}-10-18`)).toBe('15 - 18 de octubre');
        expect(formatearRangoFechas(`${anio}-10-28`, `${anio}-11-02`)).toBe('28 de octubre - 2 de noviembre');
        expect(formatearRangoFechas(`${anio}-10-15`, `${anio}-10-15`)).toBe('15 de octubre');
        expect(formatearRangoFechas('2099-10-15', '2099-10-15')).toBe('15 de octubre de 2099');
        expect(formatearRangoFechas('2099-10-15', '2099-10-18')).toBe('15 - 18 de octubre de 2099');
        expect(formatearRangoFechas('2098-12-28', '2099-01-03')).toBe('28 de diciembre de 2098 - 3 de enero de 2099');
    });

    it('agrega el día de la semana, con el año solo si no es el actual', () => {
        expect(formatearDiaConSemana('2099-10-15')).toBe('Jueves 15 de octubre de 2099');
        expect(formatearDiaConSemana('2099-10-18')).toBe('Domingo 18 de octubre de 2099');
        expect(formatearDiaConSemana('2099-10-19')).toBe('Lunes 19 de octubre de 2099');
        expect(formatearDiaConSemana(`${anio}-01-01`)).toMatch(/^[A-Z][a-zé]+ 1 de enero$/);
    });

    it('lista todos los días del viaje, cruzando de mes', () => {
        expect(diasDelViaje('2099-02-27', '2099-03-02')).toEqual(['2099-02-27', '2099-02-28', '2099-03-01', '2099-03-02']);
        expect(diasDelViaje('2099-05-10', '2099-05-10')).toEqual(['2099-05-10']);
    });

    it('no lista días si las fechas faltan o están al revés', () => {
        expect(diasDelViaje('', '2099-05-10')).toEqual([]);
        expect(diasDelViaje('2099-05-10', '')).toEqual([]);
        expect(diasDelViaje('2099-05-10', '2099-05-09')).toEqual([]);
    });
});

describe('formato de horas y duraciones', () => {
    it('muestra la hora en formato de 12 horas', () => {
        expect(formatearHora('09:00')).toBe('9:00 a. m.');
        expect(formatearHora('14:05')).toBe('2:05 p. m.');
        expect(formatearHora('12:00')).toBe('12:00 p. m.');
        expect(formatearHora('00:30')).toBe('12:30 a. m.');
    });

    it('calcula la hora de fin sin cruzar la medianoche', () => {
        expect(horaFin('09:00', 180)).toBe('12:00');
        expect(horaFin('22:30', 180)).toBe('23:59');
    });

    it('escribe la duración en palabras sencillas', () => {
        expect(formatearDuracion(60)).toBe('1 hora');
        expect(formatearDuracion(120)).toBe('2 horas');
        expect(formatearDuracion(90)).toBe('1 h 30 min');
        expect(formatearDuracion(45)).toBe('45 min');
    });

    it('etiqueta la dificultad con texto', () => {
        expect(etiquetaDificultad('BAJA')).toBe('Dificultad baja');
        expect(etiquetaDificultad('MODERADA')).toBe('Dificultad moderada');
        expect(etiquetaDificultad('ALTA')).toBe('Dificultad alta');
    });
});

describe('ordenarPorHora', () => {
    it('ordena de más temprana a más tarde sin modificar la lista original', () => {
        const original = [{ horaInicio: '14:00' }, { horaInicio: '09:30' }, { horaInicio: '09:05' }];

        expect(ordenarPorHora(original).map((a) => a.horaInicio)).toEqual(['09:05', '09:30', '14:00']);
        expect(original.map((a) => a.horaInicio)).toEqual(['14:00', '09:30', '09:05']);
    });

    it('a igual hora conserva el orden original', () => {
        const lista = [{ horaInicio: '10:00', n: 1 }, { horaInicio: '10:00', n: 2 }];

        expect(ordenarPorHora(lista).map((a) => a.n)).toEqual([1, 2]);
    });
});

describe('detectarSolapes', () => {
    it('no encuentra solapes cuando no hay actividades o hay una', () => {
        expect(detectarSolapes([])).toEqual([]);
        expect(detectarSolapes([actividad('a', '2099-10-15', '09:00', 180)])).toEqual([]);
    });

    it('detecta dos actividades que se pisan, sin importar el orden de la lista', () => {
        const solapes = detectarSolapes([actividad('b', '2099-10-15', '11:00', 120), actividad('a', '2099-10-15', '09:00', 180)]);

        expect(solapes).toHaveLength(1);
        expect(solapes[0].primera.clave).toBe('a');
        expect(solapes[0].segunda.clave).toBe('b');
    });

    it('terminar justo cuando empieza la siguiente no es solape', () => {
        expect(detectarSolapes([actividad('a', '2099-10-15', '09:00', 180), actividad('b', '2099-10-15', '12:00', 120)])).toEqual([]);
    });

    it('actividades de días distintos nunca se solapan', () => {
        expect(detectarSolapes([actividad('a', '2099-10-15', '09:00', 180), actividad('b', '2099-10-16', '09:00', 180)])).toEqual([]);
    });

    it('una actividad larga que cubre varias posteriores las detecta todas', () => {
        const solapes = detectarSolapes([
            actividad('a', '2099-10-15', '09:00', 360),
            actividad('b', '2099-10-15', '10:00', 60),
            actividad('c', '2099-10-15', '12:00', 60),
        ]);

        expect(solapes.map((s) => [s.primera.clave, s.segunda.clave])).toEqual([
            ['a', 'b'],
            ['a', 'c'],
        ]);
    });

    it('describe el solape con nombres, horas y día', () => {
        const [solape] = detectarSolapes([actividad('a', '2099-10-15', '09:00', 180), actividad('b', '2099-10-15', '11:00', 120)]);

        expect(describirSolape(solape)).toBe(
            '«Actividad a» (9:00 a. m. – 12:00 p. m.) y «Actividad b» (11:00 a. m. – 1:00 p. m.) se solapan el 15 de octubre de 2099.',
        );
    });
});
