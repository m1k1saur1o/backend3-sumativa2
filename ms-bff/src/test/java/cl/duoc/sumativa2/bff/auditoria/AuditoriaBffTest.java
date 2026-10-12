package cl.duoc.sumativa2.bff.auditoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import cl.duoc.sumativa2.bff.support.EntornoBffFalso;
import cl.duoc.sumativa2.bff.support.ServidorHttpFalso.Respuesta;
import cl.duoc.sumativa2.bff.support.ServidorHttpFalso.Solicitud;
import cl.duoc.sumativa2.bff.support.TestJwt;

/**
 * Cada llamada al BFF genera un evento BffCallEvent v1 que se envia de forma asincrona a
 * producer-service (POST /auditoria/eventos) con el token de servicio, tanto en exito como en
 * errores funcionales y de seguridad, sin incluir tokens ni datos sensibles.
 */
class AuditoriaBffTest extends EntornoBffFalso {

    private static String nuevoRequestId() {
        return "req-" + UUID.randomUUID();
    }

    private JsonNode unicoEvento(String requestId) {
        await().atMost(Duration.ofSeconds(10)).until(() -> eventosDe(requestId).size() == 1);
        return eventosDe(requestId).get(0);
    }

    @Test
    void llamadaExitosaGeneraEventoCompletoConTokenDeServicio() throws Exception {
        String requestId = nuevoRequestId();
        String jwtUsuario = TestJwt.valido("USER");

        mockMvc.perform(get("/api/movil/cuentas/110")
                        .header("Authorization", bearer(jwtUsuario))
                        .header("X-Request-Id", requestId))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", requestId));

        JsonNode e = unicoEvento(requestId);
        assertThat(e.get("schemaVersion").asText()).isEqualTo("1.0");
        assertThat(e.get("eventType").asText()).isEqualTo("BFF_HTTP_CALL");
        assertThat(UUID.fromString(e.get("eventId").asText())).isNotNull();
        assertThat(Instant.parse(e.get("timestamp").asText())).isBeforeOrEqualTo(Instant.now());
        assertThat(e.get("timestamp").asText()).endsWith("Z");
        assertThat(e.get("sourceService").asText()).isEqualTo("ms-bff");
        assertThat(e.get("method").asText()).isEqualTo("GET");
        assertThat(e.get("path").asText()).isEqualTo("/api/movil/cuentas/110");
        assertThat(e.get("routePattern").asText()).isEqualTo("/api/movil/cuentas/{cuentaId}");
        assertThat(e.get("endpoint").asText()).isEqualTo("movil.cuentas");
        assertThat(e.get("channel").asText()).isEqualTo("movil");
        assertThat(e.get("user").asText()).isEqualTo("test-user");
        assertThat(e.get("authenticated").asBoolean()).isTrue();
        assertThat(e.get("status").asInt()).isEqualTo(200);
        assertThat(e.get("outcome").asText()).isEqualTo("SUCCESS");
        assertThat(e.get("durationMs").asLong()).isGreaterThanOrEqualTo(0);
        assertThat(e.get("errorCode").isNull()).isTrue();

        // El evento no transporta el JWT del usuario ni cabeceras
        Solicitud enviada = PRODUCER.solicitudes().stream()
                .filter(s -> s.cuerpo().contains(requestId)).findFirst().orElseThrow();
        assertThat(enviada.cuerpo()).doesNotContain(jwtUsuario).doesNotContain("eyJ").doesNotContainIgnoringCase("authorization");
        assertThat(enviada.cabecera("Authorization")).isEqualTo("Bearer " + TOKEN_SERVICIO);
        assertThat(enviada.cabecera("X-Request-Id")).isEqualTo(requestId);
    }

    @Test
    void tokenDeServicioSeObtieneConClientCredentialsYSeReutiliza() throws Exception {
        String token = bearer(TestJwt.valido("USER"));
        String primero = nuevoRequestId();
        mockMvc.perform(get("/api/cajeros/cuentas/110").header("Authorization", token).header("X-Request-Id", primero));
        unicoEvento(primero);
        String segundo = nuevoRequestId();
        mockMvc.perform(get("/api/cajeros/intereses/110").header("Authorization", token).header("X-Request-Id", segundo));
        unicoEvento(segundo);

        List<Solicitud> pedidosDeToken = AUTH.solicitudes();
        assertThat(pedidosDeToken.size()).isLessThanOrEqualTo(1);
        pedidosDeToken.forEach(s -> {
            assertThat(s.ruta()).isEqualTo("/auth/token");
            assertThat(s.cuerpo()).contains("grant_type=client_credentials", "client_id=ms-bff-test");
        });
    }

    @Test
    void error404FuncionalTambienSeRegistra() throws Exception {
        String requestId = nuevoRequestId();
        mockMvc.perform(get("/api/cajeros/cuentas/99999")
                        .header("Authorization", bearer(TestJwt.valido("USER"))).header("X-Request-Id", requestId))
                .andExpect(status().isNotFound());

        JsonNode e = unicoEvento(requestId);
        assertThat(e.get("status").asInt()).isEqualTo(404);
        assertThat(e.get("outcome").asText()).isEqualTo("CLIENT_ERROR");
        assertThat(e.get("errorCode").asText()).isEqualTo("DATOS_NO_ENCONTRADOS");
        assertThat(e.get("errorMessage").asText()).contains("99999");
        assertThat(e.get("channel").asText()).isEqualTo("cajeros");
    }

    @Test
    void error400ParametroInvalidoSeRegistra() throws Exception {
        String requestId = nuevoRequestId();
        mockMvc.perform(get("/api/movil/intereses/" + "9".repeat(51))
                        .header("Authorization", bearer(TestJwt.valido("USER"))).header("X-Request-Id", requestId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        JsonNode e = unicoEvento(requestId);
        assertThat(e.get("status").asInt()).isEqualTo(400);
        assertThat(e.get("errorCode").asText()).isEqualTo("PARAMETRO_INVALIDO");
    }

    @Test
    void error503MsBackendCaidoSeRegistraComoErrorDeServidor() throws Exception {
        backends(s -> new Respuesta(500, "{\"status\":500,\"message\":\"Error interno del microservicio de datos\"}"));
        String requestId = nuevoRequestId();

        mockMvc.perform(get("/api/movil/transacciones/110")
                        .header("Authorization", bearer(TestJwt.valido("USER"))).header("X-Request-Id", requestId))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("El microservicio de datos no esta disponible en este momento"));

        JsonNode e = unicoEvento(requestId);
        assertThat(e.get("status").asInt()).isEqualTo(503);
        assertThat(e.get("outcome").asText()).isEqualTo("SERVER_ERROR");
        assertThat(e.get("errorCode").asText()).isEqualTo("SERVICIO_DATOS_NO_DISPONIBLE");
    }

    @Test
    void llamadaSinTokenSeRegistraSinUsuario() throws Exception {
        String requestId = nuevoRequestId();
        mockMvc.perform(get("/api/movil/cuentas/110").header("X-Request-Id", requestId))
                .andExpect(status().isUnauthorized());

        JsonNode e = unicoEvento(requestId);
        assertThat(e.get("status").asInt()).isEqualTo(401);
        assertThat(e.get("user").isNull()).isTrue();
        assertThat(e.get("authenticated").asBoolean()).isFalse();
        assertThat(e.get("errorCode").asText()).isEqualTo("TOKEN_AUSENTE");
        assertThat(e.get("endpoint").asText()).isEqualTo("movil.cuentas");
        assertThat(e.get("routePattern").isNull()).isTrue();
    }

    @Test
    void tokenInvalidoSeRegistraSinFiltrarElToken() throws Exception {
        String requestId = nuevoRequestId();
        String tokenAjeno = TestJwt.token("otro-secreto-distinto-0123456789abcdefgh", TestJwt.ISSUER,
                List.of(TestJwt.AUDIENCE), Instant.now(), Instant.now().plusSeconds(600), "USER");
        mockMvc.perform(get("/api/cajeros/transacciones/110")
                        .header("Authorization", bearer(tokenAjeno)).header("X-Request-Id", requestId))
                .andExpect(status().isUnauthorized());

        JsonNode e = unicoEvento(requestId);
        assertThat(e.get("errorCode").asText()).isEqualTo("TOKEN_INVALIDO");
        assertThat(e.toString()).doesNotContain(tokenAjeno);
    }

    @Test
    void accesoDenegadoSeRegistraConElUsuarioDelToken() throws Exception {
        String requestId = nuevoRequestId();
        mockMvc.perform(get("/api/cajeros/cuentas/110")
                        .header("Authorization", bearer(TestJwt.valido())).header("X-Request-Id", requestId))
                .andExpect(status().isForbidden());

        JsonNode e = unicoEvento(requestId);
        assertThat(e.get("status").asInt()).isEqualTo(403);
        assertThat(e.get("user").asText()).isEqualTo("test-user");
        assertThat(e.get("errorCode").asText()).isEqualTo("ACCESO_DENEGADO");
    }

    @Test
    void rutaConAlgoConFormaDeJwtSeSanea() throws Exception {
        String requestId = nuevoRequestId();
        String falso = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ4In0.firma";
        mockMvc.perform(get("/api/movil/cuentas/" + falso)
                .header("Authorization", bearer(TestJwt.valido("USER"))).header("X-Request-Id", requestId));

        JsonNode e = unicoEvento(requestId);
        assertThat(e.get("path").asText()).isEqualTo("/api/movil/cuentas/[REDACTED]");
        assertThat(e.toString()).doesNotContain(falso);
    }

    @Test
    void requestIdInvalidoSeReemplazaPorUnoGenerado() throws Exception {
        mockMvc.perform(get("/api/movil/cuentas/110")
                        .header("Authorization", bearer(TestJwt.valido("USER")))
                        .header("X-Request-Id", "<script>"))
                .andExpect(header().string("X-Request-Id", org.hamcrest.Matchers.matchesPattern("[0-9a-f-]{36}")));
    }

    @Test
    void producerCaidoNoAfectaLaRespuestaDelBff() throws Exception {
        PRODUCER.responder(s -> new Respuesta(503, "{\"error\":\"kafka_unavailable\"}"));
        String requestId = nuevoRequestId();

        long t0 = System.nanoTime();
        mockMvc.perform(get("/api/movil/cuentas/110")
                        .header("Authorization", bearer(TestJwt.valido("USER"))).header("X-Request-Id", requestId))
                .andExpect(status().isOk());
        assertThat(Duration.ofNanos(System.nanoTime() - t0)).isLessThan(Duration.ofSeconds(2));

        // Se intento publicar (y fallo) en segundo plano
        unicoEvento(requestId);
    }

    @Test
    void tokenDeServicioRechazadoSeRenuevaYSeReintentaUnaVez() throws Exception {
        java.util.concurrent.atomic.AtomicInteger intentos = new java.util.concurrent.atomic.AtomicInteger();
        PRODUCER.responder(s -> intentos.incrementAndGet() == 1
                ? new Respuesta(401, "{\"error\":\"invalid_token\"}")
                : new Respuesta(201, "{}"));
        String requestId = nuevoRequestId();

        mockMvc.perform(get("/api/movil/cuentas/110")
                .header("Authorization", bearer(TestJwt.valido("USER"))).header("X-Request-Id", requestId));

        await().atMost(Duration.ofSeconds(10)).until(() -> eventosDe(requestId).size() == 2);
        assertThat(AUTH.solicitudes()).isNotEmpty();
    }
}
