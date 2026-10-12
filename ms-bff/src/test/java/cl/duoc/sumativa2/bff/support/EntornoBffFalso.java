package cl.duoc.sumativa2.bff.support;

import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cl.duoc.sumativa2.bff.support.ServidorHttpFalso.Respuesta;
import cl.duoc.sumativa2.bff.support.ServidorHttpFalso.Solicitud;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

/**
 * Contexto comun: dos replicas falsas de ms-backend, un producer-service y un auth-service falsos,
 * registrados como instancias del SimpleDiscoveryClient de Spring Cloud. El BFF los resuelve con
 * el mismo LoadBalancer que usa con Eureka (en las pruebas Eureka esta deshabilitado).
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:ms-bff-test.properties")
public abstract class EntornoBffFalso {

    public static final String TOKEN_SERVICIO = "token-de-servicio-falso";

    protected static final ServidorHttpFalso BACKEND_A = new ServidorHttpFalso();
    protected static final ServidorHttpFalso BACKEND_B = new ServidorHttpFalso();
    protected static final ServidorHttpFalso PRODUCER = new ServidorHttpFalso();
    protected static final ServidorHttpFalso AUTH = new ServidorHttpFalso();

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private CircuitBreakerRegistry circuitBreakers;

    @DynamicPropertySource
    static void instancias(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.discovery.client.simple.instances[ms-backend][0].uri", BACKEND_A::url);
        registry.add("spring.cloud.discovery.client.simple.instances[ms-backend][1].uri", BACKEND_B::url);
        registry.add("spring.cloud.discovery.client.simple.instances[producer-service][0].uri", PRODUCER::url);
        registry.add("spring.cloud.discovery.client.simple.instances[auth-service][0].uri", AUTH::url);
    }

    @BeforeEach
    void restablecerEntorno() {
        for (ServidorHttpFalso s : List.of(BACKEND_A, BACKEND_B, PRODUCER, AUTH)) {
            s.limpiar();
        }
        backends(MsBackendFalso::responder);
        PRODUCER.responder(s -> new Respuesta(201, "{\"estado\":\"PUBLICADO\"}"));
        AUTH.responder(s -> new Respuesta(200,
                "{\"access_token\":\"" + TOKEN_SERVICIO + "\",\"token_type\":\"Bearer\",\"expires_in\":3600}"));
        circuitBreakers.getAllCircuitBreakers().forEach(cb -> cb.reset());
    }

    protected static void backends(Function<Solicitud, Respuesta> responder) {
        BACKEND_A.responder(responder);
        BACKEND_B.responder(responder);
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }

    /** Eventos de auditoria recibidos por producer-service con un requestId dado. */
    protected List<JsonNode> eventosDe(String requestId) {
        return PRODUCER.solicitudes().stream()
                .filter(s -> s.ruta().equals("/auditoria/eventos"))
                .map(s -> leer(s.cuerpo()))
                .filter(e -> requestId.equals(e.get("requestId").asText()))
                .toList();
    }

    private JsonNode leer(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
