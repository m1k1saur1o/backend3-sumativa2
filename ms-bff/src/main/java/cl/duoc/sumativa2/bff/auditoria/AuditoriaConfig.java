package cl.duoc.sumativa2.bff.auditoria;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import cl.duoc.sumativa2.bff.config.AuditoriaProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Registra el filtro de auditoria y elige el publicador segun AUDIT_ENABLED.
 */
@Configuration
public class AuditoriaConfig {

    /** Antes que Spring Security (orden -100): tambien audita los 401/403. */
    static final int ORDEN_FILTRO = Ordered.HIGHEST_PRECEDENCE + 10;

    @Bean
    FilterRegistrationBean<AuditoriaFilter> auditoriaFilter(AuditoriaPublisher publisher,
            @Value("${spring.application.name}") String servicio,
            @Value("${eureka.instance.instance-id:${spring.application.name}}") String instancia) {

        FilterRegistrationBean<AuditoriaFilter> registro =
                new FilterRegistrationBean<>(new AuditoriaFilter(publisher, servicio, instancia));
        registro.setOrder(ORDEN_FILTRO);
        registro.addUrlPatterns("/api/*");
        return registro;
    }

    @Bean
    @ConditionalOnProperty(name = "app.auditoria.enabled", havingValue = "true", matchIfMissing = true)
    AuditoriaPublisher producerAuditoriaPublisher(AuditoriaProperties props,
            RestClient.Builder loadBalancedRestClientBuilder, CircuitBreakerRegistry circuitBreakers,
            MeterRegistry meterRegistry) {

        if (props.clientId() == null || props.clientId().isBlank()
                || props.clientSecret() == null || props.clientSecret().isBlank()) {
            throw new IllegalStateException(
                    "AUTH_CLIENT_ID y AUTH_CLIENT_SECRET son obligatorios con AUDIT_ENABLED=true");
        }

        SimpleClientHttpRequestFactory timeouts = new SimpleClientHttpRequestFactory();
        timeouts.setConnectTimeout(props.connectTimeout());
        timeouts.setReadTimeout(props.readTimeout());

        RestClient authClient = loadBalancedRestClientBuilder.clone()
                .baseUrl(props.authUrl()).requestFactory(timeouts).build();
        RestClient producerClient = loadBalancedRestClientBuilder.clone()
                .baseUrl(props.producerUrl()).requestFactory(timeouts).build();

        return new ProducerAuditoriaPublisher(producerClient,
                new TokenServicioProvider(authClient, props.clientId(), props.clientSecret()),
                circuitBreakers.circuitBreaker("auditoria"), props.threads(), props.queueCapacity(), meterRegistry);
    }

    @Bean
    @ConditionalOnProperty(name = "app.auditoria.enabled", havingValue = "false")
    AuditoriaPublisher logAuditoriaPublisher() {
        return new LogAuditoriaPublisher();
    }
}
