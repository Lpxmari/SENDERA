package com.sendera.repository;

import com.sendera.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findByUsuarioIdOrderByIdAsc(Long usuarioId);

    Optional<Reserva> findByIdAndUsuarioId(Long id, Long usuarioId);
}
