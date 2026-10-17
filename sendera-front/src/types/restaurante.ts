/**
 * Restaurante ofertado. PROPUESTA de contrato: aún no existe en docs/openapi.yaml ni en el MER,
 * por lo que hoy los datos son simulados (ver services/restauranteService.ts).
 */
export interface Restaurante {
    id: number;
    nombre: string;
    tipoCocina: string;
    ubicacion: string;
    descripcion: string;
    precioPromedio: number; // COP por persona
    calificacion: number; // 0 a 5
    accesible: boolean;
    fotoUrl: string | null;
}
