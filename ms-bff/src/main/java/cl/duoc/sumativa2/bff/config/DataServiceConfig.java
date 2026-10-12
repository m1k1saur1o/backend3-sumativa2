package cl.duoc.sumativa2.bff.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import cl.duoc.sumativa2.bff.auditoria.CorrelacionInterceptor;

/**
 * Cliente HTTP hacia ms-backend. La URL se toma de {@code data-service.url}
 * (application.properties): es el unico punto del proyecto que la conoce.
 *
 * <p>La URL es un nombre logico ({@code http://ms-backend}): el builder {@link LoadBalanced}
 * lo resuelve en cada llamada contra el registro de Eureka y reparte las solicitudes entre las
 * replicas disponibles (round robin). No hay direcciones IP ni puertos fijos en el codigo.
 *
 * <p>Los timeouts explicitos evitan que una caida de ms-backend deje las
 * solicitudes del BFF (y sus threads) esperando indefinidamente.
 */
@Configuration
public class DataServiceConfig {

    /**
     * Builder compartido con balanceo de carga. Cada cliente debe usar {@code clone()} para no
     * modificar la configuracion de los demas.
     */
    @Bean
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }

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
                                            ClientHttpRequestFactory dataServiceRequestFactory,
                                            RestClient.Builder loadBalancedRestClientBuilder) {
        return loadBalancedRestClientBuilder.clone()
                .baseUrl(baseUrl)
                .requestFactory(dataServiceRequestFactory)
                .requestInterceptor(new CorrelacionInterceptor())
                .build();
    }
}
