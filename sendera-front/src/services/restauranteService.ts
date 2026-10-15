import { Restaurante } from '../types/restaurante';
import { restaurantesSimulados } from '../utils/datosRestaurantes';

/*
 * SIMULADO: todavía no hay endpoint de restaurantes en docs/openapi.yaml.
 * Al conectarlo, solo cambia el cuerpo por una llamada a apiClient (p. ej. GET /restaurantes);
 * la firma y el tipo se mantienen.
 */
export const fetchRestaurantes = async (): Promise<Restaurante[]> => restaurantesSimulados;
