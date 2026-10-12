package cl.duoc.sumativa2.bff.balanceo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.net.ServerSocket;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import cl.duoc.sumativa2.bff.support.MsBackendFalso;
import cl.duoc.sumativa2.bff.support.ServidorHttpFalso;
import cl.duoc.sumativa2.bff.support.TestJwt;

/**
 * Una replica registrada esta caida (conexion rechazada): el LoadBalancer reintenta en la otra y
 * el cliente no ve el error.
 */
@SpringBootTest(properties = "app.auditoria.enabled=false")
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:ms-bff-test.properties")
class FailoverMsBackendTest {

    private static final ServidorHttpFalso VIVA = new ServidorHttpFalso();

    static {
        VIVA.responder(MsBackendFalso::responder);
    }

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void instancias(DynamicPropertyRegistry registry) throws IOException {
        int puertoLibre;
        try (ServerSocket s = new ServerSocket(0)) {
            puertoLibre = s.getLocalPort();
        }
        String caida = "http://127.0.0.1:" + puertoLibre;
        registry.add("spring.cloud.discovery.client.simple.instances[ms-backend][0].uri", () -> caida);
        registry.add("spring.cloud.discovery.client.simple.instances[ms-backend][1].uri", VIVA::url);
    }

    @Test
    void replicaCaidaNoAfectaAlCliente() throws Exception {
        String token = "Bearer " + TestJwt.valido("USER");
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(get("/api/cajeros/cuentas/110").header("Authorization", token))
                    .andExpect(status().isOk());
        }
        assertThat(VIVA.solicitudes()).hasSize(4);
    }
}
