package com.sendera.controller;

import com.sendera.config.SecurityConfig;
import com.sendera.dto.AuthRespuestaDto;
import com.sendera.dto.UsuarioDto;
import com.sendera.exception.ContrasenaActualIncorrectaException;
import com.sendera.exception.CorreoYaRegistradoException;
import com.sendera.exception.GlobalExceptionHandler;
import com.sendera.model.Usuario;
import com.sendera.security.CuentaActivaVerificador;
import com.sendera.security.JwtService;
import com.sendera.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RF-004: PUT /auth/me exige sesión, toma la identidad del token y nunca acepta rol desde el cliente. */
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, JwtService.class})
@TestPropertySource(properties = {
        "app.jwt.secret=clave-de-pruebas-con-mas-de-treinta-y-dos-caracteres",
        "app.jwt.expiracion-horas=1",
        "app.cors.allowed-origins=http://localhost:5173"
})
class PerfilApiTest {

    private static final String CUERPO = """
            {"nombre":"María","apellidos":"Gutiérrez","correo":"mariana@correo.com","contrasenaActual":"clave1234"}""";

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @MockBean AuthService authService;
    @MockBean CuentaActivaVerificador cuentaActiva;

    @BeforeEach
    void cuentaActiva() {
        when(cuentaActiva.estaActiva(7L)).thenReturn(true);
    }

    private String token() {
        return "Bearer " + jwtService.generar(Usuario.builder().id(7L).correo("maria@correo.com").rol("ROLE_CLIENTE").build());
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(put("/auth/me").contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void actualizaUsandoElIdentificadorDelToken() throws Exception {
        when(authService.actualizarPerfil(eq(7L), any())).thenReturn(AuthRespuestaDto.builder().token("jwt-nuevo")
                .usuario(UsuarioDto.builder().id(7L).nombre("María").apellidos("Gutiérrez").correo("mariana@correo.com").rol("ROLE_CLIENTE").build()).build());

        mockMvc.perform(put("/auth/me").header("Authorization", token()).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-nuevo"))
                .andExpect(jsonPath("$.usuario.correo").value("mariana@correo.com"));

        verify(authService).actualizarPerfil(eq(7L), any());
    }

    @Test
    void ignoraUnRolEnviadoPorElCliente() throws Exception {
        when(authService.actualizarPerfil(eq(7L), any())).thenReturn(AuthRespuestaDto.builder().token("t")
                .usuario(UsuarioDto.builder().id(7L).rol("ROLE_CLIENTE").build()).build());

        mockMvc.perform(put("/auth/me").header("Authorization", token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"María\",\"apellidos\":\"G\",\"correo\":\"maria@correo.com\",\"rol\":\"ROLE_ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.rol").value("ROLE_CLIENTE"));
    }

    @Test
    void datosInvalidosDevuelven400() throws Exception {
        for (String cuerpo : new String[]{
                "{\"nombre\":\"\",\"apellidos\":\"G\",\"correo\":\"a@b.com\"}",
                "{\"nombre\":\"M\",\"apellidos\":\"\",\"correo\":\"a@b.com\"}",
                "{\"nombre\":\"M\",\"apellidos\":\"G\",\"correo\":\"no-es-correo\"}",
                "{}"}) {
            mockMvc.perform(put("/auth/me").header("Authorization", token()).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void contrasenaActualIncorrectaDevuelve400ConMensajeClaro() throws Exception {
        when(authService.actualizarPerfil(eq(7L), any())).thenThrow(new ContrasenaActualIncorrectaException());

        mockMvc.perform(put("/auth/me").header("Authorization", token()).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Para cambiar tu correo escribe tu contraseña actual correctamente."));
    }

    @Test
    void correoYaRegistradoDevuelve409() throws Exception {
        when(authService.actualizarPerfil(eq(7L), any())).thenThrow(new CorreoYaRegistradoException());

        mockMvc.perform(put("/auth/me").header("Authorization", token()).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isConflict());
    }
}
