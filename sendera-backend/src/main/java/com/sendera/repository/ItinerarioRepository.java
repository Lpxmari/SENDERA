package com.sendera.repository;

import com.sendera.model.Itinerario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ItinerarioRepository extends JpaRepository<Itinerario, Long> {

    List<Itinerario> findByDestinoIdIn(Collection<Long> destinoIds);

    boolean existsByDestinoId(Long destinoId);
}
