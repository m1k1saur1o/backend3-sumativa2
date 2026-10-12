package cl.duoc.sumativa2.bff.balanceo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import cl.duoc.sumativa2.bff.support.TestJwt;

/**
 * Sin ninguna instancia de ms-backend registrada el BFF responde el 503 de siempre (no un 500).
 */
@SpringBootTest(properties = "app.auditoria.enabled=false")
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:ms-bff-test.properties")
class SinInstanciasMsBackendTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void sinInstanciasDevuelve503() throws Exception {
        mockMvc.perform(get("/api/movil/cuentas/110").header("Authorization", "Bearer " + TestJwt.valido("USER")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message").value("El microservicio de datos no esta disponible en este momento"));
    }
}
