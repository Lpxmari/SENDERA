package com.sendera.repository;

import com.sendera.dto.FiltroDestinoDto;
import com.sendera.model.Destino;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Condiciones de búsqueda de destinos que se resuelven directamente en la base de datos. */
public final class DestinoSpecifications {

    /** Carácter de escape para LIKE (no se usa '\' porque MySQL lo trata como escape del literal). */
    private static final char ESCAPE = '!';

    private DestinoSpecifications() {
    }

    public static Specification<Destino> conFiltros(FiltroDestinoDto filtro) {
        return (root, query, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();

            String texto = filtro.getQ();
            if (texto != null && !texto.isBlank()) {
                String patron = "%" + escaparLike(texto.trim().toLowerCase(Locale.ROOT)) + "%";
                condiciones.add(cb.or(
                        cb.like(cb.lower(root.<String>get("nombre")), patron, ESCAPE),
                        cb.like(cb.lower(root.<String>get("ubicacion")), patron, ESCAPE),
                        cb.like(cb.lower(root.<String>get("descripcion")), patron, ESCAPE)));
            }
            if (filtro.getPrecioMin() != null) {
                condiciones.add(cb.greaterThanOrEqualTo(root.<BigDecimal>get("precioBase"), filtro.getPrecioMin()));
            }
            if (filtro.getPrecioMax() != null) {
                condiciones.add(cb.lessThanOrEqualTo(root.<BigDecimal>get("precioBase"), filtro.getPrecioMax()));
            }
            return cb.and(condiciones.toArray(new Predicate[0]));
        };
    }

    private static String escaparLike(String texto) {
        return texto.replace("" + ESCAPE, ESCAPE + "" + ESCAPE)
                .replace("%", ESCAPE + "%")
                .replace("_", ESCAPE + "_");
    }
}
