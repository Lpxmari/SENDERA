package com.sendera.repository;

import com.sendera.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreo(String correo);

    boolean existsByCorreo(String correo);

    boolean existsByIdAndActivoTrue(Long id);

    long countByRolAndActivoTrue(String rol);

    /** Búsqueda administrativa. {@code patron} ya viene en minúsculas, con comodines y con '!' como carácter de escape. */
    @Query("""
            select u from Usuario u
            where (:patron is null
                   or lower(u.nombre) like :patron escape '!'
                   or lower(u.apellidos) like :patron escape '!'
                   or lower(u.correo) like :patron escape '!')
              and (:rol is null or u.rol = :rol)
              and (:activo is null or u.activo = :activo)
            """)
    Page<Usuario> buscar(@Param("patron") String patron, @Param("rol") String rol,
                         @Param("activo") Boolean activo, Pageable pageable);
}
