package com.sendera.service.impl;

import com.sendera.model.AuditoriaAdmin;
import com.sendera.repository.AuditoriaAdminRepository;
import com.sendera.security.UsuarioActual;
import com.sendera.service.AuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditoriaServiceImpl implements AuditoriaService {

    private static final int MAX_DETALLE = 500;

    private final AuditoriaAdminRepository repository;

    /** Se une a la transacción de la operación: si esta falla, tampoco queda un registro de algo que no ocurrió. */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void registrar(UsuarioActual actor, String accion, String recurso, Long recursoId, String detalle) {
        repository.save(AuditoriaAdmin.builder()
                .adminId(actor.id())
                .accion(accion)
                .recurso(recurso)
                .recursoId(recursoId)
                .detalle(detalle == null ? null : recortar(detalle))
                .build());
    }

    private String recortar(String detalle) {
        return detalle.length() <= MAX_DETALLE ? detalle : detalle.substring(0, MAX_DETALLE);
    }
}
