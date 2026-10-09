package com.sendera.repository;

import com.sendera.model.TokenRecuperacion;
import com.sendera.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TokenRecuperacionRepository extends JpaRepository<TokenRecuperacion, Long> {

    Optional<TokenRecuperacion> findByTokenHash(String tokenHash);

    void deleteByUsuario(Usuario usuario);
}
