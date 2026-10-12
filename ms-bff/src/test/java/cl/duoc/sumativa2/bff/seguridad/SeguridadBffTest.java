package cl.duoc.sumativa2.bff.seguridad;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import cl.duoc.sumativa2.bff.support.EntornoBffFalso;
import cl.duoc.sumativa2.bff.support.TestJwt;

/**
 * ms-bff como OAuth2 Resource Server: /api/** exige JWT valido (firma, issuer, audience,
 * expiracion) con rol USER o ADMIN; /actuator/health es publico.
 */
class SeguridadBffTest extends EntornoBffFalso {

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/movil/cuentas/110", "/api/movil/intereses/110", "/api/movil/transacciones/110",
            "/api/cajeros/cuentas/110", "/api/cajeros/intereses/110", "/api/cajeros/transacciones/110",
            "/api/web/cuentas/110" })
    void sinTokenDevuelve401(String ruta) throws Exception {
        mockMvc.perform(get(ruta))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", containsString("Bearer")))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value(containsString("Falta el token")));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/movil/cuentas/110", "/api/movil/intereses/110", "/api/movil/transacciones/110" })
    void tokenValidoEnMovilDevuelveLista(String ruta) throws Exception {
        mockMvc.perform(get(ruta).header("Authorization", bearer(TestJwt.valido("USER"))))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0]").exists());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/cajeros/cuentas/110", "/api/cajeros/intereses/110", "/api/cajeros/transacciones/110" })
    void tokenValidoEnCajerosDevuelveUnSoloObjeto(String ruta) throws Exception {
        mockMvc.perform(get(ruta).header("Authorization", bearer(TestJwt.valido("ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isMap());
    }

    @Test
    void contratoDeRespuestaSeConservaEnMovilYCajeros() throws Exception {
        String token = bearer(TestJwt.valido("USER"));
        mockMvc.perform(get("/api/movil/cuentas/110").header("Authorization", token))
                .andExpect(jsonPath("$[0].fecha").value("2024-12-22"))
                .andExpect(jsonPath("$[0].transaccion").value("deposito"))
                .andExpect(jsonPath("$[0].descripcion").doesNotExist());
        mockMvc.perform(get("/api/cajeros/intereses/110").header("Authorization", token))
                .andExpect(jsonPath("$.nombre").value("Charlie Green"))
                .andExpect(jsonPath("$.saldo").value(10000.0))
                .andExpect(jsonPath("$.tipo").doesNotExist());
    }

    @Test
    void tokenMalformadoDevuelve401() throws Exception {
        mockMvc.perform(get("/api/movil/cuentas/110").header("Authorization", bearer("esto-no-es-un-jwt")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(containsString("Token invalido")));
    }

    @Test
    void firmaConOtroSecretoDevuelve401() throws Exception {
        String token = TestJwt.token("otro-secreto-distinto-0123456789abcdefgh", TestJwt.ISSUER,
                List.of(TestJwt.AUDIENCE), Instant.now(), Instant.now().plusSeconds(600), "USER");
        mockMvc.perform(get("/api/movil/cuentas/110").header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenExpiradoDevuelve401() throws Exception {
        String token = TestJwt.token(TestJwt.SECRET, TestJwt.ISSUER, List.of(TestJwt.AUDIENCE),
                Instant.now().minusSeconds(7200), Instant.now().minusSeconds(3600), "USER");
        mockMvc.perform(get("/api/cajeros/cuentas/110").header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", containsString("expired")));
    }

    @Test
    void issuerIncorrectoDevuelve401() throws Exception {
        String token = TestJwt.token(TestJwt.SECRET, "otro-issuer", List.of(TestJwt.AUDIENCE),
                Instant.now(), Instant.now().plusSeconds(600), "USER");
        mockMvc.perform(get("/api/movil/intereses/110").header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void audienceDeOtraApiDevuelve401() throws Exception {
        // Token valido para producer/consumer (notificaciones-api) pero no emitido para el BFF
        String token = TestJwt.token(TestJwt.SECRET, TestJwt.ISSUER, List.of("test-audience"),
                Instant.now(), Instant.now().plusSeconds(600), "USER");
        mockMvc.perform(get("/api/movil/transacciones/110").header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenSinRolesDevuelve403() throws Exception {
        mockMvc.perform(get("/api/movil/cuentas/110").header("Authorization", bearer(TestJwt.valido())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void tokenDeServicioAuditPublisherDevuelve403() throws Exception {
        mockMvc.perform(get("/api/cajeros/cuentas/110")
                        .header("Authorization", bearer(TestJwt.valido("AUDIT_PUBLISHER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("roles")));
    }

    @Test
    void healthEsPublico() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void otrasRutasNoEstanExpuestas() throws Exception {
        mockMvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/movil/inexistente/110").header("Authorization", bearer(TestJwt.valido("USER"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
