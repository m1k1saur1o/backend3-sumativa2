package cl.duoc.sumativa2.bff.auditoria;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Genera un evento de auditoria por cada llamada a {@code /api/**}, incluidas las rechazadas por
 * seguridad (401/403) y las que terminan en error funcional (400/404/503/500). Se registra antes
 * que Spring Security, por lo que mide la duracion total de la solicitud.
 *
 * <p>La publicacion es asincrona ({@link AuditoriaPublisher}): la respuesta al cliente no espera a
 * producer-service ni a Kafka. El evento no incluye cabeceras, tokens, query string ni cuerpos, y la
 * ruta y el mensaje de error se sanean por si contienen algo con forma de token.
 */
public class AuditoriaFilter extends OncePerRequestFilter {

    private static final Pattern RUTA = Pattern.compile("^/api/([^/]+)/([^/]+)(?:/.*)?$");
    private static final Pattern REQUEST_ID_VALIDO = Pattern.compile("[A-Za-z0-9._-]{8,64}");
    private static final Pattern JWT = Pattern.compile("eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]*");
    private static final Pattern BEARER = Pattern.compile("(?i)bearer\\s+[A-Za-z0-9._~+/=-]+");
    private static final Set<String> CANALES = Set.of("web", "movil", "cajeros");
    private static final Set<String> RECURSOS = Set.of("cuentas", "intereses", "transacciones");
    private static final int MAX_PATH = 2048;
    private static final int MAX_MENSAJE = 500;

    private final AuditoriaPublisher publisher;
    private final String sourceService;
    private final String sourceInstance;

    public AuditoriaFilter(AuditoriaPublisher publisher, String sourceService, String sourceInstance) {
        this.publisher = publisher;
        this.sourceService = sourceService;
        this.sourceInstance = sourceInstance;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        Instant inicio = Instant.now();
        long t0 = System.nanoTime();
        String requestId = resolverRequestId(request.getHeader(AuditoriaAtributos.HEADER_REQUEST_ID));
        response.setHeader(AuditoriaAtributos.HEADER_REQUEST_ID, requestId);
        MDC.put(AuditoriaAtributos.MDC_REQUEST_ID, requestId);

        boolean excepcion = false;
        try {
            chain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException ex) {
            excepcion = true;
            throw ex;
        } finally {
            long duracionMs = (System.nanoTime() - t0) / 1_000_000;
            int status = excepcion && response.getStatus() < 400 ? 500 : response.getStatus();
            publisher.publicar(construirEvento(request, inicio, requestId, status, duracionMs));
            MDC.remove(AuditoriaAtributos.MDC_REQUEST_ID);
        }
    }

    private BffCallEvent construirEvento(HttpServletRequest request, Instant inicio, String requestId, int status,
                                         long duracionMs) {

        String path = sanear(request.getRequestURI(), MAX_PATH);
        String canal = "desconocido";
        String recurso = "desconocido";
        Matcher m = RUTA.matcher(request.getRequestURI());
        if (m.matches()) {
            canal = CANALES.contains(m.group(1)) ? m.group(1) : canal;
            recurso = RECURSOS.contains(m.group(2)) ? m.group(2) : recurso;
        }

        Object patron = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String usuario = (String) request.getAttribute(AuditoriaAtributos.USUARIO);

        String errorCodigo = null;
        String errorMensaje = null;
        if (status >= 400) {
            errorCodigo = (String) request.getAttribute(AuditoriaAtributos.ERROR_CODIGO);
            errorMensaje = (String) request.getAttribute(AuditoriaAtributos.ERROR_MENSAJE);
            if (errorCodigo == null) {
                HttpStatus conocido = HttpStatus.resolve(status);
                errorCodigo = "HTTP_" + status;
                errorMensaje = conocido != null ? conocido.getReasonPhrase() : null;
            }
        }

        return new BffCallEvent(
                BffCallEvent.SCHEMA_VERSION,
                BffCallEvent.EVENT_TYPE,
                UUID.randomUUID().toString(),
                inicio.toString(),
                requestId,
                sourceService,
                sourceInstance,
                request.getMethod(),
                path,
                patron instanceof String p && p.startsWith("/api/") ? p : null,
                canal + "." + recurso,
                canal,
                usuario,
                usuario != null,
                status,
                BffCallEvent.outcomeDe(status),
                duracionMs,
                errorCodigo,
                sanear(errorMensaje, MAX_MENSAJE));
    }

    static String resolverRequestId(String recibido) {
        return recibido != null && REQUEST_ID_VALIDO.matcher(recibido).matches()
                ? recibido
                : UUID.randomUUID().toString();
    }

    static String sanear(String texto, int largoMaximo) {
        if (texto == null) {
            return null;
        }
        String limpio = BEARER.matcher(JWT.matcher(texto).replaceAll("[REDACTED]")).replaceAll("Bearer [REDACTED]");
        return limpio.length() <= largoMaximo ? limpio : limpio.substring(0, largoMaximo);
    }
}
