package cl.duoc.sumativa2.bff.auditoria;

/**
 * Evento de auditoria de una llamada HTTP recibida por ms-bff (contrato v1, tópico bff-call-events).
 *
 * <ul>
 * <li>{@code schemaVersion} "1.0" y {@code eventType} "BFF_HTTP_CALL" identifican el contrato.</li>
 * <li>{@code timestamp}: instante UTC (ISO-8601) en que el BFF recibio la solicitud.</li>
 * <li>{@code requestId}: correlacion (cabecera X-Request-Id recibida o generada).</li>
 * <li>{@code endpoint}: nombre logico canal.recurso (ej. movil.cuentas); {@code channel}: web, movil o cajeros.</li>
 * <li>{@code user}: claim sub del JWT validado; null si la solicitud no se autentico.</li>
 * <li>{@code outcome}: SUCCESS (&lt;400), CLIENT_ERROR (4xx) o SERVER_ERROR (5xx).</li>
 * <li>{@code errorCode} / {@code errorMessage}: solo cuando status &gt;= 400.</li>
 * </ul>
 *
 * <p>Nunca incluye cabeceras, tokens, contraseñas, query string ni cuerpos. Esquema completo y
 * reglas de versionado: docs/EVENTO_AUDITORIA_BFF.md (repositorio sumativa3-backend3).
 */
public record BffCallEvent(
        String schemaVersion,
        String eventType,
        String eventId,
        String timestamp,
        String requestId,
        String sourceService,
        String sourceInstance,
        String method,
        String path,
        String routePattern,
        String endpoint,
        String channel,
        String user,
        boolean authenticated,
        int status,
        String outcome,
        long durationMs,
        String errorCode,
        String errorMessage) {

    public static final String SCHEMA_VERSION = "1.0";
    public static final String EVENT_TYPE = "BFF_HTTP_CALL";

    public static String outcomeDe(int status) {
        return status < 400 ? "SUCCESS" : status < 500 ? "CLIENT_ERROR" : "SERVER_ERROR";
    }
}
