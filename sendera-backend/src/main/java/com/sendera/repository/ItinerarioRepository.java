package com.sendera.repository;

import com.sendera.model.Itinerario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItinerarioRepository extends JpaRepository<Itinerario, Long> {
}