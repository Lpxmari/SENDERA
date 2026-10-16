import { CategoriaViaje, SugerenciaViaje } from '../types/planificador';

export const categoriasViaje: CategoriaViaje[] = [
    'Recorridos Históricos',
    'Comida y Cocina',
    'Relajación Costera',
    'Avistamiento de Fauna',
];

// Datos de ejemplo del mockup. Reemplazar por una consulta al backend cuando exista el campo `categoria`.
export const sugerenciasViaje: SugerenciaViaje[] = [
    { id: 1, nombre: 'Ciudad Amurallada', tipo: 'Centro Histórico', lugar: 'Cartagena', categoria: 'Recorridos Históricos', imagenUrl: '/imagenes/sugerencias/cartagena.jpg' },
    { id: 2, nombre: 'Barichara', tipo: 'Pueblo patrimonio', lugar: 'Santander', categoria: 'Recorridos Históricos', imagenUrl: '/imagenes/sugerencias/barichara.jpg' },
    { id: 3, nombre: 'Fogón Paisa', tipo: 'Restaurante de comida típica', lugar: 'Medellín', categoria: 'Comida y Cocina', imagenUrl: '/imagenes/restaurantes/fogon-paisa.jpg' },
    { id: 4, nombre: 'Casa del Puerto', tipo: 'Restaurante de mar', lugar: 'Cartagena', categoria: 'Comida y Cocina', imagenUrl: '/imagenes/restaurantes/casa-del-puerto.jpg' },
    { id: 5, nombre: 'Cabo San Juan', tipo: 'Playa en el Parque Tayrona', lugar: 'Santa Marta', categoria: 'Relajación Costera', imagenUrl: '/imagenes/destinos/tayrona-1.jpg' },
    { id: 6, nombre: 'Parque de los Nevados', tipo: 'Reserva Natural', lugar: 'Caldas', categoria: 'Avistamiento de Fauna', imagenUrl: '/imagenes/destinos/nevados-1.jpg' },
];
