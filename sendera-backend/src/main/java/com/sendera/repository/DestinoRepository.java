package com.sendera.repository;

import com.sendera.model.Destino;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DestinoRepository extends JpaRepository<Destino, Long>, JpaSpecificationExecutor<Destino> {
}