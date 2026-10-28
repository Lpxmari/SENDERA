package com.sendera.repository;

import com.sendera.model.Viaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ViajeRepository extends JpaRepository<Viaje, Long> {

    /** Solo los viajes de la persona indicada, con actividades y destinos en una sola consulta (evita N+1). */
    @Query("select distinct v from Viaje v "
            + "left join fetch v.actividades va left join fetch va.actividad a left join fetch a.destino "
            + "where v.usuario.id = :usuarioId order by v.fechaInicio asc, v.id asc")
    List<Viaje> findConActividadesByUsuarioId(@Param("usuarioId") Long usuarioId);

    /** El filtro por propietario va en la consulta: un viaje ajeno es indistinguible de uno inexistente. */
    @Query("select v from Viaje v "
            + "left join fetch v.actividades va left join fetch va.actividad a left join fetch a.destino "
            + "where v.id = :id and v.usuario.id = :usuarioId")
    Optional<Viaje> findConActividadesByIdAndUsuarioId(@Param("id") Long id, @Param("usuarioId") Long usuarioId);
}
