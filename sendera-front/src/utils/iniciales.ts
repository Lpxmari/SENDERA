/** "Ana María Pérez" → "AM". Usa las dos primeras palabras; con una sola, su primera letra. */
export const iniciales = (nombreCompleto: string): string => {
    const palabras = nombreCompleto.trim().split(/\s+/).filter(Boolean);
    if (palabras.length === 0) return '?';
    return palabras
        .slice(0, 2)
        .map((palabra) => palabra.charAt(0))
        .join('')
        .toUpperCase();
};
