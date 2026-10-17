package com.sendera.service;

import com.sendera.dto.*;

public interface AuthService {

    AuthRespuestaDto registrar(RegistroDto dto);

    AuthRespuestaDto iniciarSesion(LoginDto dto);

    /** Siempre responde igual exista o no el correo, para no revelar qué cuentas existen. */
    void solicitarRecuperacion(RecuperarContrasenaDto dto);

    void cambiarContrasena(CambiarContrasenaDto dto);

    UsuarioDto obtenerPerfil(String correo);

    /**
     * Actualiza nombre, apellidos y correo del propio usuario. Devuelve una sesión nueva porque el correo
     * es el sujeto (sub) del JWT: el token anterior deja de coincidir con la cuenta si el correo cambió.
     */
    AuthRespuestaDto actualizarPerfil(Long usuarioId, ActualizarPerfilDto dto);
}
