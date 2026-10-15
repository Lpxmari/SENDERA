package com.sendera.repository;

import com.sendera.model.Itinerario;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ItinerarioRepository extends JpaRepository<Itinerario, Long> {

    List<Itinerario> findByDestinoIdIn(Collection<Long> destinoIds);

    /** Bloquea la fila para que dos reservas simultáneas no consuman el mismo cupo. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Itinerario i where i.id = :id")
    Optional<Itinerario> buscarParaReservar(@Param("id") Long id);

    boolean existsByDestinoId(Long destinoId);
}
