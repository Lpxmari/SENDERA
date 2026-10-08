import { CategoriaViaje, SugerenciaViaje } from '../types/planificador';

export const categoriasViaje: CategoriaViaje[] = [
    'Recorridos Históricos',
    'Comida y Cocina',
    'Relajación Costera',
    'Avistamiento de Fauna',
];

// Datos de ejemplo del mockup. Reemplazar por una consulta al backend cuando exista el campo `categoria`.
export const sugerenciasViaje: SugerenciaViaje[] = [
    { id: 1, nombre: 'Iglesia San Juan', tipo: 'Centro Histórico', lugar: 'Mayorca', categoria: 'Recorridos Históricos' },
    { id: 2, nombre: 'Karigakure', tipo: 'Ciudad Antigua', lugar: 'Hamburgo', categoria: 'Recorridos Históricos' },
    { id: 3, nombre: 'Eleanor', tipo: 'Restaurante de alta cocina', lugar: 'Bogotá', categoria: 'Comida y Cocina' },
    { id: 4, nombre: 'Palm Beach', tipo: 'Restaurante de mar', lugar: 'Panamá City', categoria: 'Comida y Cocina' },
    { id: 5, nombre: 'Coral', tipo: 'Resort village en la playa', lugar: 'Isla de Jeju', categoria: 'Relajación Costera' },
    { id: 6, nombre: 'Parque de los Nevados', tipo: 'Reserva Natural', lugar: 'Quindío', categoria: 'Avistamiento de Fauna' },
];