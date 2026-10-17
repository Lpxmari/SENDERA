package com.sendera.repository;

import java.time.LocalDateTime;

/** Proyección para el listado de moderación: evita cargar la imagen (LONGBLOB) de cada reseña. */
public interface ResenaAdminResumen {
    Long getId();
    Long getDestinoId();
    String getDestinoNombre();
    Long getUsuarioId();
    String getAutor();
    String getComentario();
    Integer getCalificacion();
    Boolean getTieneImagen();
    LocalDateTime getCreadaEn();
}
