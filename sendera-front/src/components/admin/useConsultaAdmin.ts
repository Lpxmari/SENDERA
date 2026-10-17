import { useEffect, useRef, useState } from 'react';
import { obtenerMensajeError } from '../../services/apiClient';

interface Resultado<T> {
    clave: string;
    datos: T | null;
    error: string | null;
}

/**
 * Consulta que se repite cada vez que cambia `clave` (filtros, página, recarga).
 * Mientras la clave del último resultado no coincide con la actual, `cargando` es true;
 * los datos anteriores se conservan para que la tabla no parpadee. Una respuesta que llega
 * tarde, de una consulta ya reemplazada, se descarta.
 */
export const useConsultaAdmin = <T>(clave: string, consulta: () => Promise<T>, mensajeError: string) => {
    const [resultado, setResultado] = useState<Resultado<T> | null>(null);
    const consultaActual = useRef(consulta);

    useEffect(() => {
        consultaActual.current = consulta;
    });

    useEffect(() => {
        let vigente = true;
        consultaActual
            .current()
            .then((datos) => {
                if (vigente) setResultado({ clave, datos, error: null });
            })
            .catch((err: unknown) => {
                if (vigente)
                    setResultado((previo) => ({
                        clave,
                        datos: previo?.datos ?? null,
                        error: obtenerMensajeError(err, mensajeError),
                    }));
            });
        return () => {
            vigente = false;
        };
    }, [clave, mensajeError]);

    return {
        datos: resultado?.datos ?? null,
        cargando: resultado?.clave !== clave,
        error: resultado?.clave === clave ? resultado.error : null,
    };
};
