package com.sendera.service;

import com.sendera.security.UsuarioActual;

public interface AuditoriaService {

    /**
     * Registra una acción administrativa en la misma transacción de la operación auditada.
     * El detalle debe describir el cambio sin incluir contraseñas, tokens ni datos personales sensibles.
     */
    void registrar(UsuarioActual actor, String accion, String recurso, Long recursoId, String detalle);
}
