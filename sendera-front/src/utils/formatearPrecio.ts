const formateador = new Intl.NumberFormat('es-CO', {
    style: 'currency',
    currency: 'COP',
    maximumFractionDigits: 0,
});

export const formatearPrecio = (valor: number): string => formateador.format(valor);