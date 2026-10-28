package com.sendera.repository;

import com.sendera.model.Actividad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ActividadRepository extends JpaRepository<Actividad, Long> {

    @Query("select a from Actividad a join fetch a.destino order by a.nombre asc, a.id asc")
    List<Actividad> findTodas();

    @Query("select a from Actividad a join fetch a.destino d where d.id = :destinoId order by a.nombre asc, a.id asc")
    List<Actividad> findPorDestino(@Param("destinoId") Long destinoId);

    @Query("select a from Actividad a join fetch a.destino where a.id = :id")
    Optional<Actividad> findConDestinoById(@Param("id") Long id);

    @Query("select a from Actividad a join fetch a.destino where a.id in :ids")
    List<Actividad> findConDestinoByIdIn(@Param("ids") Collection<Long> ids);
}
