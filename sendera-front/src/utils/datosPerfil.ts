import { OpinionViajero, PerfilViajero, ViajeHistorial } from '../types/perfil';

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

export const historialSimulado: ViajeHistorial[] = [
    { id: 1, destino: 'Valle del Cocora', fechaReserva: '2026-08-12', montoTotal: 450000, estadoReserva: 'CONFIRMADA' },
    {
        id: 2,
        destino: 'Cartagena histórica',
        fechaReserva: '2026-05-03',
        montoTotal: 980000,
        estadoReserva: 'CONFIRMADA',
    },
    { id: 3, destino: 'Eje Cafetero', fechaReserva: '2026-02-20', montoTotal: 620000, estadoReserva: 'CANCELADA' },
];
