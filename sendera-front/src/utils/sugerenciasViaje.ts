import { CategoriaViaje, SugerenciaViaje } from '../types/planificador';

export const categoriasViaje: CategoriaViaje[] = [
    'Recorridos Históricos',
    'Comida y Cocina',
    'Relajación Costera',
    'Avistamiento de Fauna',
];

// Datos de ejemplo del mockup. Reemplazar por una consulta al backend cuando exista el campo `categoria`.
export const sugerenciasViaje: SugerenciaViaje[] = [
    { id: 1, destinoId: 1, nombre: 'Hotel Boutique Casa Aurora', tipo: 'Casona colonial', lugar: 'Cartagena', categoria: 'Recorridos Históricos', imagenUrl: '/imagenes/alojamientos/casa-aurora-1.jpg' },
    { id: 2, destinoId: 4, nombre: 'Posada Colonial El Aljibe', tipo: 'Posada con patio', lugar: 'Villa de Leyva', categoria: 'Recorridos Históricos', imagenUrl: '/imagenes/alojamientos/el-aljibe-1.jpg' },
    { id: 3, destinoId: 2, nombre: 'Finca Hotel Los Arrayanes', tipo: 'Desayuno campesino incluido', lugar: 'Salento', categoria: 'Comida y Cocina', imagenUrl: '/imagenes/alojamientos/los-arrayanes-2.jpg' },
    { id: 4, destinoId: 4, nombre: 'Posada El Aljibe', tipo: 'Desayuno casero en el patio', lugar: 'Villa de Leyva', categoria: 'Comida y Cocina', imagenUrl: '/imagenes/alojamientos/el-aljibe-2.jpg' },
    { id: 5, destinoId: 3, nombre: 'Cabañas Playa Dorada', tipo: 'Cabañas frente al mar', lugar: 'Santa Marta', categoria: 'Relajación Costera', imagenUrl: '/imagenes/alojamientos/playa-dorada-1.jpg' },
    { id: 6, destinoId: 5, nombre: 'Hotel Mirador del Lago', tipo: 'Hotel junto al lago', lugar: 'Jardín', categoria: 'Avistamiento de Fauna', imagenUrl: '/imagenes/alojamientos/mirador-del-lago-1.jpg' },
];
