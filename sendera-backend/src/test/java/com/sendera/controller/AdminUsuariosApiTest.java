package com.sendera.controller;

import com.sendera.config.SecurityConfig;
import com.sendera.dto.PaginaDto;
import com.sendera.dto.UsuarioAdminDto;
import com.sendera.exception.GlobalExceptionHandler;
import com.sendera.exception.OperacionAdminNoPermitidaException;
import com.sendera.model.Usuario;
import com.sendera.security.CuentaActivaVerificador;
import com.sendera.security.JwtService;
import com.sendera.security.UsuarioActual;
import com.sendera.service.UsuarioAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RF-005/RF-013: /admin/** exige ROLE_ADMIN y una cuenta activa; la identidad sale del token. */
@WebMvcTest(UsuarioAdminController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, JwtService.class})
@TestPropertySource(properties = {
        "app.jwt.secret=clave-de-pruebas-con-mas-de-treinta-y-dos-caracteres",
        "app.jwt.expiracion-horas=1",
        "app.cors.allowed-origins=http://localhost:5173"
})
class AdminUsuariosApiTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    JwtService jwtService;
    @MockBean
    UsuarioAdminService usuarioAdminService;
    @MockBean
    CuentaActivaVerificador cuentaActiva;

    @BeforeEach
    void cuentasActivas() {
        when(cuentaActiva.estaActiva(7L)).thenReturn(true);
    }

    private String token(String rol) {
        return jwtService.generar(Usuario.builder().id(7L).correo("a@b.com").rol(rol).build());
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/admin/usuarios"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void comoClienteDevuelve403() throws Exception {
        mockMvc.perform(get("/admin/usuarios").header("Authorization", "Bearer " + token("ROLE_CLIENTE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void comoAdminListaUsuarios() throws Exception {
        when(usuarioAdminService.listar(null, null, null, 0, 20)).thenReturn(PaginaDto.<UsuarioAdminDto>builder()
                .contenido(List.of(UsuarioAdminDto.builder().id(1L).correo("x@y.com").rol("ROLE_CLIENTE").activo(true).build()))
                .pagina(0).tamano(20).total(1).build());

        mockMvc.perform(get("/admin/usuarios").header("Authorization", "Bearer " + token("ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.contenido[0].correo").value("x@y.com"));
    }

    @Test
    void cuentaDesactivadaDevuelve401AunConTokenVigente() throws Exception {
        when(cuentaActiva.estaActiva(7L)).thenReturn(false);

        mockMvc.perform(get("/admin/usuarios").header("Authorization", "Bearer " + token("ROLE_ADMIN")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cambiarRolTomaLaIdentidadDelToken() throws Exception {
        when(usuarioAdminService.cambiarRol(eq(2L), any(), any())).thenReturn(UsuarioAdminDto.builder().id(2L).rol("ROLE_ADMIN").build());

        mockMvc.perform(patch("/admin/usuarios/2/rol").header("Authorization", "Bearer " + token("ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rol\":\"ROLE_ADMIN\"}"))
                .andExpect(status().isOk());

        verify(usuarioAdminService).cambiarRol(eq(2L), any(), eq(new UsuarioActual(7L, true)));
    }

    @Test
    void rolInvalidoDevuelve400() throws Exception {
        mockMvc.perform(patch("/admin/usuarios/2/rol").header("Authorization", "Bearer " + token("ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rol\":\"ROLE_ROOT\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cambiarActivoSinCuerpoValidoDevuelve400() throws Exception {
        mockMvc.perform(patch("/admin/usuarios/2/activo").header("Authorization", "Bearer " + token("ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void operacionNoPermitidaDevuelve409() throws Exception {
        when(usuarioAdminService.cambiarActivo(eq(7L), any(), any()))
                .thenThrow(new OperacionAdminNoPermitidaException("No puedes desactivar tu propia cuenta."));

        mockMvc.perform(patch("/admin/usuarios/7/activo").header("Authorization", "Bearer " + token("ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":false}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("No puedes desactivar tu propia cuenta."));
    }

    @Test
    void comoClienteNoPuedeCambiarRoles() throws Exception {
        mockMvc.perform(patch("/admin/usuarios/2/rol").header("Authorization", "Bearer " + token("ROLE_CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rol\":\"ROLE_ADMIN\"}"))
                .andExpect(status().isForbidden());
    }
}
