package cl.duoc.sumativa2.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Contrato REST que consume ms-bff y health usado por Docker y Eureka (H2 en lugar de MySQL).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MsBackendApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEsPublicoYReportaUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void cuentasAnualesOrdenadasPorFechaDesc() throws Exception {
        mockMvc.perform(get("/api/cuentas-anuales/110"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fecha").value("2024-12-22"))
                .andExpect(jsonPath("$[1].transaccion").value("compra"));
    }

    @Test
    void sinRegistrosDevuelve404() throws Exception {
        mockMvc.perform(get("/api/intereses/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void identificadorDemasiadoLargoDevuelve400() throws Exception {
        mockMvc.perform(get("/api/transacciones/" + "9".repeat(51)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
