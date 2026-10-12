package cl.duoc.sumativa2.bff.auditoria;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AuditoriaFilterTest {

    @Test
    void saneaJwtYBearerEnTextosLibres() {
        assertThat(AuditoriaFilter.sanear("x eyJa.eyJb.c y", 500)).isEqualTo("x [REDACTED] y");
        assertThat(AuditoriaFilter.sanear("Authorization: Bearer abc.def", 500))
                .isEqualTo("Authorization: Bearer [REDACTED]");
        assertThat(AuditoriaFilter.sanear("a".repeat(600), 500)).hasSize(500);
        assertThat(AuditoriaFilter.sanear(null, 10)).isNull();
    }

    @Test
    void aceptaRequestIdValidoYGeneraUnoNuevoSiNoLoEs() {
        assertThat(AuditoriaFilter.resolverRequestId("abc-12345_x.y")).isEqualTo("abc-12345_x.y");
        assertThat(AuditoriaFilter.resolverRequestId(null)).hasSize(36);
        assertThat(AuditoriaFilter.resolverRequestId("corto")).hasSize(36);
        assertThat(AuditoriaFilter.resolverRequestId("a b c d e f g h")).hasSize(36);
    }

    @Test
    void outcomeSegunStatus() {
        assertThat(BffCallEvent.outcomeDe(200)).isEqualTo("SUCCESS");
        assertThat(BffCallEvent.outcomeDe(404)).isEqualTo("CLIENT_ERROR");
        assertThat(BffCallEvent.outcomeDe(503)).isEqualTo("SERVER_ERROR");
    }
}
