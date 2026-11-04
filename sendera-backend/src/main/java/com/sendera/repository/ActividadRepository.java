package com.sendera.repository;

import com.sendera.model.Actividad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ActividadRepository extends JpaRepository<Actividad, Long> {

    /** Filtros opcionales (null = sin filtro); los límites de duración son inclusivos. */
    @Query("""
            select a from Actividad a join fetch a.destino d
            where (:destinoId is null or d.id = :destinoId)
              and (:minMinutos is null or a.duracionMinutos >= :minMinutos)
              and (:maxMinutos is null or a.duracionMinutos <= :maxMinutos)
            order by a.nombre asc, a.id asc
            """)
    List<Actividad> buscar(@Param("destinoId") Long destinoId,
                           @Param("minMinutos") Integer minMinutos,
                           @Param("maxMinutos") Integer maxMinutos);

    @Query("select a from Actividad a join fetch a.destino where a.id = :id")
    Optional<Actividad> findConDestinoById(@Param("id") Long id);

    @Query("select a from Actividad a join fetch a.destino where a.id in :ids")
    List<Actividad> findConDestinoByIdIn(@Param("ids") Collection<Long> ids);
}
