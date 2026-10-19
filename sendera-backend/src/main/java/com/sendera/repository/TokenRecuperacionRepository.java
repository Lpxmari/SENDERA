package com.sendera.repository;

import com.sendera.model.TokenRecuperacion;
import com.sendera.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TokenRecuperacionRepository extends JpaRepository<TokenRecuperacion, Long> {

    Optional<TokenRecuperacion> findFirstByUsuarioOrderByIdDesc(Usuario usuario);

    void deleteByUsuario(Usuario usuario);

    @Modifying
    @Query("update TokenRecuperacion t set t.intentos = t.intentos + 1 where t.id = :id and t.intentos < :maximo")
    int consumirIntento(@Param("id") Long id, @Param("maximo") int maximo);

    @Modifying
    @Query("update TokenRecuperacion t set t.intentos = t.intentos - 1 where t.id = :id and t.intentos > 0")
    int devolverIntento(@Param("id") Long id);
}
