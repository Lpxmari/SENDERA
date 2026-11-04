import { OpinionViajero, PerfilViajero } from '../types/perfil';

/** DATOS SIMULADOS del perfil: se eliminan cuando exista el endpoint de perfil en docs/openapi.yaml. */
export const perfilSimulado = (base: Pick<PerfilViajero, 'nombre' | 'apellidos' | 'correo'>): PerfilViajero => ({
    nombreUsuario: '@viajero_sendera',
    nivelViajero: 'Viajero experto',
    fotoUrl: null,
    numeroIdentificacion: '123111233',
    fechaNacimiento: '1965-03-23',
    paisResidencia: 'Colombia',
    destinosFavoritos: ['París', 'Buenos Aires', 'Barcelona', 'Cartagena'],
    intereses: ['Tours históricos', 'Arte', 'Playas', 'Restaurantes'],
    ...base,
});

export const opinionesSimuladas: OpinionViajero[] = [
    { id: 1, texto: 'Una experiencia muy agradable' },
    { id: 2, texto: 'Fue fácil organizar mi viaje' },
    { id: 3, texto: 'Encontré un lugar perfecto para descansar' },
];
