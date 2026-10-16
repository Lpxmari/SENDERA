package com.sendera.repository;

import com.sendera.model.Resena;
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
}
