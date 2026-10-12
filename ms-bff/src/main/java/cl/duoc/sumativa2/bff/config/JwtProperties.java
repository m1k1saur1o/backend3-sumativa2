package cl.duoc.sumativa2.bff.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Validacion de los JWT emitidos por auth-service: mismo secreto HS256 (JWT_SECRET) e issuer
 * (JWT_ISSUER), y la audiencia propia del BFF (BFF_JWT_AUDIENCE). El BFF no arranca sin secreto.
 */
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        @NotBlank(message = "JWT_SECRET es obligatorio")
        @Size(min = 32, message = "JWT_SECRET debe tener al menos 32 caracteres (256 bits)") String secret,
        @NotBlank String issuer,
        @NotBlank String audience) {

    @Override
    public String toString() {
        return "JwtProperties[secret=****, issuer=" + issuer + ", audience=" + audience + "]";
    }
}
