package com.sendera.repository;

import com.sendera.model.Reserva;
import com.sendera.model.EstadoReserva;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

import java.util.List;
import java.util.Optional;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findByUsuarioIdOrderByIdAsc(Long usuarioId);

    Optional<Reserva> findByIdAndUsuarioId(Long id, Long usuarioId);

    /**
     * Listado administrativo con filtros opcionales. {@code patron} viene en minúsculas, con comodines y con '!'
     * como carácter de escape. Carga usuario, itinerario y destino en la misma consulta (sin N+1).
     */
    @EntityGraph(attributePaths = {"usuario", "itinerario", "itinerario.destino"})
    @Query("""
            select r from Reserva r
            join r.usuario u
            join r.itinerario i
            join i.destino d
            where (:estado is null or r.estadoReserva = :estado)
              and (:desde is null or r.fechaReserva >= :desde)
              and (:hasta is null or r.fechaReserva <= :hasta)
              and (:patron is null
                   or lower(u.nombre) like :patron escape '!'
                   or lower(u.apellidos) like :patron escape '!'
                   or lower(u.correo) like :patron escape '!'
                   or lower(d.nombre) like :patron escape '!'
                   or lower(i.nombre) like :patron escape '!')
            """)
    Page<Reserva> buscarAdmin(@Param("estado") EstadoReserva estado, @Param("desde") LocalDate desde,
                              @Param("hasta") LocalDate hasta, @Param("patron") String patron, Pageable pageable);

    long countByEstadoReserva(EstadoReserva estado);
}
