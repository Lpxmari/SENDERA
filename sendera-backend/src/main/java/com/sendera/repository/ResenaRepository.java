package com.sendera.repository;

import com.sendera.model.Resena;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ResenaRepository extends JpaRepository<Resena, Long> {

    @Query("""
            select r.id as id,
                   d.id as destinoId,
                   r.comentario as comentario,
                   r.calificacion as calificacion,
                   concat(concat(u.nombre, ' '), u.apellidos) as autor,
                   r.creadaEn as creadaEn,
                   case when r.imagen is null then false else true end as tieneImagen
            from Resena r
            join r.destino d
            join r.usuario u
            where d.id = :destinoId
            order by r.creadaEn desc, r.id desc
            """)
    List<ResenaResumen> buscarResumenPorDestino(@Param("destinoId") Long destinoId);

    Optional<Resena> findByIdAndDestinoId(Long id, Long destinoId);

    /** Listado de moderación. {@code patron} en minúsculas, con comodines y '!' como escape; no carga la imagen. */
    @Query(value = """
            select r.id as id,
                   d.id as destinoId,
                   d.nombre as destinoNombre,
                   u.id as usuarioId,
                   concat(concat(u.nombre, ' '), u.apellidos) as autor,
                   r.comentario as comentario,
                   r.calificacion as calificacion,
                   case when r.imagen is null then false else true end as tieneImagen,
                   r.creadaEn as creadaEn
            from Resena r
            join r.destino d
            join r.usuario u
            where (:destinoId is null or d.id = :destinoId)
              and (:calificacionMax is null or r.calificacion <= :calificacionMax)
              and (:patron is null
                   or lower(r.comentario) like :patron escape '!'
                   or lower(u.nombre) like :patron escape '!'
                   or lower(u.apellidos) like :patron escape '!')
            """,
            countQuery = """
            select count(r) from Resena r
            join r.destino d
            join r.usuario u
            where (:destinoId is null or d.id = :destinoId)
              and (:calificacionMax is null or r.calificacion <= :calificacionMax)
              and (:patron is null
                   or lower(r.comentario) like :patron escape '!'
                   or lower(u.nombre) like :patron escape '!'
                   or lower(u.apellidos) like :patron escape '!')
            """)
    Page<ResenaAdminResumen> buscarAdmin(@Param("destinoId") Long destinoId,
                                         @Param("calificacionMax") Integer calificacionMax,
                                         @Param("patron") String patron, Pageable pageable);
}
