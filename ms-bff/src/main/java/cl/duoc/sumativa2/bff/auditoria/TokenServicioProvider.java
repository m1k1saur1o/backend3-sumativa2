package cl.duoc.sumativa2.bff.auditoria;

import java.time.Instant;

import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Obtiene y cachea el token de servicio de ms-bff con el grant client_credentials de auth-service
 * ({@code POST http://auth-service/auth/token}). Se renueva antes de expirar y se invalida si
 * producer-service lo rechaza con 401. El secreto y el token nunca se registran en logs.
 */
public class TokenServicioProvider {

    private final RestClient authClient;
    private final String clientId;
    private final String clientSecret;
    private String token;
    private Instant renovarDesde = Instant.EPOCH;

    public TokenServicioProvider(RestClient authClient, String clientId, String clientSecret) {
        this.authClient = authClient;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public synchronized String obtener() {

        if (token != null && Instant.now().isBefore(renovarDesde)) {
            return token;
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);

        TokenResponse respuesta = authClient.post()
                .uri("/auth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);

        if (respuesta == null || respuesta.accessToken() == null) {
            throw new IllegalStateException("auth-service no entrego access_token");
        }

        // Margen de renovacion: 60 s o la mitad de la vigencia si el token es muy corto
        long margen = Math.min(60, respuesta.expiresIn() / 2);
        token = respuesta.accessToken();
        renovarDesde = Instant.now().plusSeconds(respuesta.expiresIn() - margen);
        return token;
    }

    public synchronized void invalidar() {
        token = null;
    }

    record TokenResponse(@JsonProperty("access_token") String accessToken,
                         @JsonProperty("expires_in") long expiresIn) {
    }
}
