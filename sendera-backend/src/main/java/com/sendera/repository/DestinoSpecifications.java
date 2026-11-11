package com.sendera.repository;

import com.sendera.dto.FiltroDestinoDto;
import com.sendera.model.Destino;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
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
            condiciones.addAll(contiene(cb, root.<String>get("nombre"), filtro.getNombre()));
            condiciones.addAll(contiene(cb, root.<String>get("ubicacion"), filtro.getUbicacion()));
            if (filtro.getPrecioMin() != null) {
                condiciones.add(cb.greaterThanOrEqualTo(root.<BigDecimal>get("precioBase"), filtro.getPrecioMin()));
            }
            if (filtro.getPrecioMax() != null) {
                condiciones.add(cb.lessThanOrEqualTo(root.<BigDecimal>get("precioBase"), filtro.getPrecioMax()));
            }

            // --- AÑADIDO PARA EL PROCESO 2 ---
            if (filtro.getDificultad() != null && !filtro.getDificultad().isBlank()) {
                condiciones.add(cb.equal(root.<String>get("dificultad"), filtro.getDificultad()));
            }
            if (filtro.getAccesibilidad() != null) {
                condiciones.add(cb.equal(root.<Boolean>get("accesibilidad"), filtro.getAccesibilidad()));
            }

            return cb.and(condiciones.toArray(new Predicate[0]));
        };
    }

    /** Condición "contiene" sin distinguir mayúsculas; vacía si no se envió texto. */
    private static List<Predicate> contiene(CriteriaBuilder cb, Expression<String> campo, String texto) {
        if (texto == null || texto.isBlank()) {
            return List.of();
        }
        String patron = "%" + escaparLike(texto.trim().toLowerCase(Locale.ROOT)) + "%";
        return List.of(cb.like(cb.lower(campo), patron, ESCAPE));
    }

    private static String escaparLike(String texto) {
        return texto.replace("" + ESCAPE, ESCAPE + "" + ESCAPE)
                .replace("%", ESCAPE + "%")
                .replace("_", ESCAPE + "_");
    }
}