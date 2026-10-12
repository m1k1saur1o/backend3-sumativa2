package cl.duoc.sumativa2.bff.balanceo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import cl.duoc.sumativa2.bff.support.EntornoBffFalso;
import cl.duoc.sumativa2.bff.support.ServidorHttpFalso.Respuesta;
import cl.duoc.sumativa2.bff.support.TestJwt;

/**
 * El BFF llama a http://ms-backend (nombre logico) y Spring Cloud LoadBalancer reparte entre las
 * instancias registradas. Tambien verifica el circuit breaker hacia ms-backend.
 */
class BalanceoMsBackendTest extends EntornoBffFalso {

    @Autowired
    private DiscoveryClient discoveryClient;

    @Test
    void msBackendSeResuelvePorNombreLogicoConDosInstancias() {
        assertThat(discoveryClient.getInstances("ms-backend")).hasSize(2);
    }

    @Test
    void reparteLasSolicitudesEntreLasDosReplicas() throws Exception {
        String token = bearer(TestJwt.valido("USER"));
        for (int i = 0; i < 6; i++) {
            mockMvc.perform(get("/api/movil/cuentas/110").header("Authorization", token))
                    .andExpect(status().isOk());
        }

        assertThat(BACKEND_A.solicitudes()).hasSize(3);
        assertThat(BACKEND_B.solicitudes()).hasSize(3);
        assertThat(BACKEND_A.solicitudes().get(0).ruta()).isEqualTo("/api/cuentas-anuales/110");
    }

    @Test
    void propagaElRequestIdAMsBackend() throws Exception {
        mockMvc.perform(get("/api/cajeros/transacciones/110")
                        .header("Authorization", bearer(TestJwt.valido("USER")))
                        .header("X-Request-Id", "correlacion-12345"))
                .andExpect(status().isOk());

        var todas = new java.util.ArrayList<>(BACKEND_A.solicitudes());
        todas.addAll(BACKEND_B.solicitudes());
        assertThat(todas).hasSize(1);
        assertThat(todas.get(0).cabecera("X-Request-Id")).isEqualTo("correlacion-12345");
    }

    @Test
    void replicaQueResponde503SeReintentaEnLaOtra() throws Exception {
        BACKEND_A.responder(s -> new Respuesta(503, "{}"));
        String token = bearer(TestJwt.valido("USER"));

        for (int i = 0; i < 4; i++) {
            mockMvc.perform(get("/api/movil/intereses/110").header("Authorization", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].nombre").value("Charlie Green"));
        }
        assertThat(BACKEND_B.solicitudes()).hasSize(4);
    }

    @Test
    void circuitBreakerSeAbreTrasFallasRepetidasYNoContactaMsBackend() throws Exception {
        backends(s -> new Respuesta(500, "{\"status\":500}"));
        String token = bearer(TestJwt.valido("USER"));

        // minimum-number-of-calls=5 con 100 % de fallas: el circuito se abre
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/movil/cuentas/110").header("Authorization", token))
                    .andExpect(status().isServiceUnavailable());
        }
        int llamadasAntes = BACKEND_A.solicitudes().size() + BACKEND_B.solicitudes().size();

        mockMvc.perform(get("/api/movil/cuentas/110").header("Authorization", token))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("El microservicio de datos no esta disponible en este momento"));

        assertThat(BACKEND_A.solicitudes().size() + BACKEND_B.solicitudes().size()).isEqualTo(llamadasAntes);
    }

    @Test
    void erroresFuncionalesNoAbrenElCircuito() throws Exception {
        String token = bearer(TestJwt.valido("USER"));
        for (int i = 0; i < 8; i++) {
            mockMvc.perform(get("/api/movil/cuentas/99999").header("Authorization", token))
                    .andExpect(status().isNotFound());
        }
        mockMvc.perform(get("/api/movil/cuentas/110").header("Authorization", token))
                .andExpect(status().isOk());
    }
}
