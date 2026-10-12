package cl.duoc.sumativa2.bff.auditoria;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Productor de los eventos de auditoria del BFF: los envia de forma asincrona a
 * {@code POST http://producer-service/auditoria/eventos} (resuelto por Eureka + LoadBalancer) con
 * el token de servicio de ms-bff; producer-service los publica en el topico bff-call-events.
 *
 * <p>Decision ante fallos (best effort, sin bloquear al cliente):
 * <ul>
 * <li>Cola acotada (AUDIT_QUEUE_CAPACITY) y pocos hilos: si se llena, el evento se descarta.</li>
 * <li>Circuit breaker "auditoria": si producer-service/auth-service/Kafka no responden, se abre y
 * los eventos siguientes se descartan de inmediato, sin consumir hilos esperando timeouts.</li>
 * <li>Todo evento descartado queda escrito completo en el log (WARN "AUDITORIA_RESPALDO"), que
 * actua como registro de respaldo; no contiene datos sensibles.</li>
 * <li>Metricas Micrometer {@code bff.auditoria.eventos{resultado=publicado|fallido|descartado}}.</li>
 * </ul>
 */
public class ProducerAuditoriaPublisher implements AuditoriaPublisher, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(ProducerAuditoriaPublisher.class);

    private final RestClient producerClient;
    private final TokenServicioProvider tokens;
    private final CircuitBreaker circuitBreaker;
    private final ThreadPoolExecutor executor;
    private final Counter publicados;
    private final Counter fallidos;
    private final Counter descartados;

    public ProducerAuditoriaPublisher(RestClient producerClient, TokenServicioProvider tokens,
                                      CircuitBreaker circuitBreaker, int hilos, int capacidadCola,
                                      MeterRegistry meterRegistry) {
        this.producerClient = producerClient;
        this.tokens = tokens;
        this.circuitBreaker = circuitBreaker;
        AtomicInteger contador = new AtomicInteger();
        this.executor = new ThreadPoolExecutor(hilos, hilos, 60, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(capacidadCola), r -> {
                    Thread t = new Thread(r, "auditoria-" + contador.incrementAndGet());
                    t.setDaemon(true);
                    return t;
                }, new ThreadPoolExecutor.AbortPolicy());
        this.publicados = contador(meterRegistry, "publicado");
        this.fallidos = contador(meterRegistry, "fallido");
        this.descartados = contador(meterRegistry, "descartado");
    }

    private static Counter contador(MeterRegistry registry, String resultado) {
        return Counter.builder("bff.auditoria.eventos").tag("resultado", resultado)
                .description("Eventos de auditoria del BFF por resultado de publicacion").register(registry);
    }

    @Override
    public void publicar(BffCallEvent evento) {
        try {
            executor.execute(() -> enviar(evento));
        } catch (RejectedExecutionException ex) {
            descartados.increment();
            log.warn("AUDITORIA_RESPALDO motivo=cola_llena evento={}", evento);
        }
    }

    private void enviar(BffCallEvent evento) {
        try {
            circuitBreaker.executeRunnable(() -> enviarConToken(evento));
            publicados.increment();
            log.debug("Evento de auditoria publicado eventId={} requestId={}", evento.eventId(), evento.requestId());
        } catch (CallNotPermittedException ex) {
            fallidos.increment();
            log.warn("AUDITORIA_RESPALDO motivo=circuito_abierto evento={}", evento);
        } catch (RuntimeException ex) {
            fallidos.increment();
            log.warn("AUDITORIA_RESPALDO motivo={} evento={}", ex.getClass().getSimpleName(), evento);
        }
    }

    private void enviarConToken(BffCallEvent evento) {
        try {
            try {
                post(evento, tokens.obtener());
            } catch (HttpClientErrorException.Unauthorized ex) {
                // Token vencido o secreto rotado: se obtiene uno nuevo y se reintenta una sola vez
                tokens.invalidar();
                post(evento, tokens.obtener());
            }
        } catch (HttpClientErrorException ex) {
            throw ex; // 4xx: error de configuracion o de contrato, no de disponibilidad
        } catch (RuntimeException ex) {
            throw new AuditoriaNoDisponibleException("Publicacion de auditoria no disponible", ex);
        }
    }

    private void post(BffCallEvent evento, String token) {
        producerClient.post()
                .uri("/auditoria/eventos")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header(AuditoriaAtributos.HEADER_REQUEST_ID, evento.requestId())
                .body(evento)
                .retrieve()
                .toBodilessEntity();
    }

    /** Al detener el BFF se intenta enviar lo que quede en la cola durante unos segundos. */
    @Override
    public void destroy() throws InterruptedException {
        executor.shutdown();
        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
            log.warn("Se detiene ms-bff con {} eventos de auditoria pendientes", executor.getQueue().size());
            executor.shutdownNow();
        }
    }
}
