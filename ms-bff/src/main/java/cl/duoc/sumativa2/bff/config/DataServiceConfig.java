package cl.duoc.sumativa2.bff.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Cliente HTTP hacia ms-backend. La URL se toma de {@code data-service.url}
 * (application.properties): es el unico punto del proyecto que la conoce.
 *
 * <p>Los timeouts explicitos evitan que una caida de ms-backend deje las
 * solicitudes del BFF (y sus threads) esperando indefinidamente.
 */
@Configuration
public class DataServiceConfig {

    @Bean
    public ClientHttpRequestFactory dataServiceRequestFactory(
            @Value("${data-service.connect-timeout-ms:2000}") int connectTimeoutMs,
            @Value("${data-service.read-timeout-ms:3000}") int readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        return factory;
    }

    @Bean
    public RestClient dataServiceRestClient(@Value("${data-service.url}") String baseUrl,
                                            ClientHttpRequestFactory dataServiceRequestFactory) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(dataServiceRequestFactory)
                .build();
    }
}
