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

/** Separa "YYYY-MM-DD" a mano: new Date("YYYY-MM-DD") se interpreta en UTC y puede mostrar el día anterior. */
const partes = (iso: string) => {
    const [anio, mes, dia] = iso.split('-').map(Number);
    return { anio, mes, dia };
};

/** "18 de noviembre de 2026" */
export const formatearFecha = (iso: string): string => {
    const { anio, mes, dia } = partes(iso);
    return `${dia} de ${MESES[mes - 1]} de ${anio}`;
};

/** "18 al 19 de noviembre de 2026" (o con ambos meses/años si difieren). */
export const formatearRangoFechas = (inicio: string, fin: string): string => {
    const a = partes(inicio);
    const b = partes(fin);
    if (inicio === fin) return formatearFecha(inicio);
    if (a.anio === b.anio && a.mes === b.mes) return `${a.dia} al ${b.dia} de ${MESES[b.mes - 1]} de ${b.anio}`;
    if (a.anio === b.anio) return `${a.dia} de ${MESES[a.mes - 1]} al ${formatearFecha(fin)}`;
    return `${formatearFecha(inicio)} al ${formatearFecha(fin)}`;
};

/** Hoy en la zona horaria del navegador, como "YYYY-MM-DD" (para el atributo min de un input de fecha). */
export const hoyIso = (): string => {
    const ahora = new Date();
    const mes = String(ahora.getMonth() + 1).padStart(2, '0');
    const dia = String(ahora.getDate()).padStart(2, '0');
    return `${ahora.getFullYear()}-${mes}-${dia}`;
};

/** Las fechas ISO (YYYY-MM-DD) se comparan bien como texto. */
export const fechaMinimaReserva = (fechaInicio: string, hoy: string = hoyIso()): string =>
    fechaInicio > hoy ? fechaInicio : hoy;
