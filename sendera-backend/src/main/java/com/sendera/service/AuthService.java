package com.sendera.service;

import com.sendera.dto.*;

public interface AuthService {

    AuthRespuestaDto registrar(RegistroDto dto);

    AuthRespuestaDto iniciarSesion(LoginDto dto);

    /** Siempre responde igual exista o no el correo, para no revelar qué cuentas existen. */
    void solicitarRecuperacion(RecuperarContrasenaDto dto);

    void cambiarContrasena(CambiarContrasenaDto dto);

    UsuarioDto obtenerPerfil(String correo);
}
