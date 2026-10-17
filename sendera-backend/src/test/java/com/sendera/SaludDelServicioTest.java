package com.sendera;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El despliegue (CD) comprueba /api/v1/actuator/health: debe ser público y mínimo.
 * Cualquier otro endpoint de Actuator debe seguir cerrado y no exponerse.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SaludDelServicioTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void healthEsPublicoYNoRevelaDetalles() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    void otrosEndpointsDeActuatorNoSeExponen() throws Exception {
        for (String ruta : new String[]{"/actuator/env", "/actuator/beans", "/actuator/heapdump", "/actuator/metrics"}) {
            // Sin token: la seguridad responde 401 antes de saber si el endpoint existe
            mockMvc.perform(get(ruta)).andExpect(status().isUnauthorized());
        }
    }
}
