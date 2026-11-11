package com.sendera.controller;

import com.sendera.config.SecurityConfig;
import com.sendera.dto.ActualizarPerfilDto;
import com.sendera.dto.AuthRespuestaDto;
import com.sendera.dto.PerfilDto;
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
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RF-004 / NC-01: GET y PUT /auth/me exigen sesión, toman la identidad del token, nunca aceptan rol desde el cliente
 * y validan los datos del perfil del viajero.
 */
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

    // ---- NC-01: datos del perfil del viajero ----

    @Test
    void getMeDevuelveElPerfilDelViajero() throws Exception {
        when(authService.obtenerPerfil("maria@correo.com")).thenReturn(PerfilDto.builder().id(7L).nombre("María")
                .apellidos("Gutiérrez").correo("maria@correo.com").rol("ROLE_CLIENTE").numeroIdentificacion("1094123456")
                .fechaNacimiento(LocalDate.of(1958, 3, 23)).paisResidencia("Colombia")
                .destinosFavoritos(List.of("Cartagena", "Salento")).intereses(List.of("Naturaleza")).build());

        mockMvc.perform(get("/auth/me").header("Authorization", token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroIdentificacion").value("1094123456"))
                .andExpect(jsonPath("$.fechaNacimiento").value("1958-03-23"))
                .andExpect(jsonPath("$.paisResidencia").value("Colombia"))
                .andExpect(jsonPath("$.destinosFavoritos[1]").value("Salento"))
                .andExpect(jsonPath("$.intereses[0]").value("Naturaleza"))
                .andExpect(jsonPath("$.contrasenaHash").doesNotExist());
    }

    @Test
    void getMeSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void putMeRecibeLosDatosDelViajero() throws Exception {
        when(authService.actualizarPerfil(eq(7L), any())).thenReturn(AuthRespuestaDto.builder().token("t")
                .usuario(UsuarioDto.builder().id(7L).rol("ROLE_CLIENTE").build()).build());

        mockMvc.perform(put("/auth/me").header("Authorization", token()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"María","apellidos":"G","correo":"maria@correo.com",
                                 "numeroIdentificacion":"1094123456","fechaNacimiento":"1958-03-23",
                                 "paisResidencia":"Colombia","destinosFavoritos":["Cartagena"],"intereses":["Naturaleza"]}"""))
                .andExpect(status().isOk());

        ArgumentCaptor<ActualizarPerfilDto> enviado = ArgumentCaptor.forClass(ActualizarPerfilDto.class);
        verify(authService).actualizarPerfil(eq(7L), enviado.capture());
        assertEquals("1094123456", enviado.getValue().getNumeroIdentificacion());
        assertEquals(LocalDate.of(1958, 3, 23), enviado.getValue().getFechaNacimiento());
        assertEquals("Colombia", enviado.getValue().getPaisResidencia());
        assertEquals(List.of("Cartagena"), enviado.getValue().getDestinosFavoritos());
        assertEquals(List.of("Naturaleza"), enviado.getValue().getIntereses());
    }

    @Test
    void datosDelViajeroInvalidosDevuelven400ConMensajeClaro() throws Exception {
        String base = "\"nombre\":\"M\",\"apellidos\":\"G\",\"correo\":\"a@b.com\",";
        String manana = LocalDate.now().plusDays(1).toString();
        String onceDestinos = "[" + "\"d\",".repeat(10) + "\"d\"]";
        String[][] casos = {
                {"\"numeroIdentificacion\":\"12ab45\"", "El número de identificación debe tener entre 5 y 15 dígitos."},
                {"\"numeroIdentificacion\":\"1234\"", "El número de identificación debe tener entre 5 y 15 dígitos."},
                {"\"fechaNacimiento\":\"" + manana + "\"", "La fecha de nacimiento no puede ser futura."},
                {"\"paisResidencia\":\"   \"", "Indica tu país de residencia."},
                {"\"destinosFavoritos\":" + onceDestinos, "Puedes guardar hasta 10 destinos favoritos."},
                {"\"destinosFavoritos\":[\"Cartagena\",\" \"]", "Los destinos favoritos no pueden estar vacíos."},
                {"\"intereses\":[\"" + "x".repeat(61) + "\"]", "Cada interés puede tener hasta 60 caracteres."},
        };
        for (String[] caso : casos) {
            mockMvc.perform(put("/auth/me").header("Authorization", token()).contentType(MediaType.APPLICATION_JSON)
                            .content("{" + base + caso[0] + "}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(caso[1]));
        }
        verify(authService, never()).actualizarPerfil(any(), any());
    }

    @Test
    void correoYaRegistradoDevuelve409() throws Exception {
        when(authService.actualizarPerfil(eq(7L), any())).thenThrow(new CorreoYaRegistradoException());

        mockMvc.perform(put("/auth/me").header("Authorization", token()).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isConflict());
    }
}
