package com.sendera.repository;

import com.sendera.model.Pago;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    Optional<Pago> findByReservaId(Long reservaId);

    /** Para listar reservas con su estado de pago en una sola consulta (sin N+1). */
    List<Pago> findByReservaIdIn(Collection<Long> reservaIds);

    /** Bloquea la fila: la notificación de Mercado Pago y la verificación del usuario pueden llegar a la vez. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pago p join fetch p.reserva where p.reserva.id = :reservaId")
    Optional<Pago> buscarParaActualizar(@Param("reservaId") Long reservaId);
}
