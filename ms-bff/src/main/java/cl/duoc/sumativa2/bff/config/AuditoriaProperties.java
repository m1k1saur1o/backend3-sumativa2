package cl.duoc.sumativa2.bff.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Publicacion de eventos de auditoria hacia producer-service.
 *
 * <p>{@code producerUrl} y {@code authUrl} son nombres logicos (http://producer-service,
 * http://auth-service) que resuelve Spring Cloud LoadBalancer. El token de servicio se obtiene
 * con client_credentials (AUTH_CLIENT_ID / AUTH_CLIENT_SECRET).
 */
@Validated
@ConfigurationProperties(prefix = "app.auditoria")
public record AuditoriaProperties(
        boolean enabled,
        @NotBlank String producerUrl,
        @NotBlank String authUrl,
        String clientId,
        String clientSecret,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout,
        @Min(1) @Max(16) int threads,
        @Min(1) @Max(100000) int queueCapacity) {

    @Override
    public String toString() {
        return "AuditoriaProperties[enabled=" + enabled + ", producerUrl=" + producerUrl + ", authUrl=" + authUrl
                + ", clientId=" + clientId + ", clientSecret=****, threads=" + threads
                + ", queueCapacity=" + queueCapacity + "]";
    }
}
