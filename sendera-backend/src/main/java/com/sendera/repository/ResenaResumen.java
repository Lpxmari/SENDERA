package com.sendera.repository;

import java.time.LocalDateTime;

public interface ResenaResumen {
    Long getId();
    Long getDestinoId();
    String getComentario();
    Integer getCalificacion();
    String getAutor();
    LocalDateTime getCreadaEn();
    Boolean getTieneImagen();
}
