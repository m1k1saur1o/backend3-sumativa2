package cl.duoc.sumativa2.bff.auditoria;

/**
 * producer-service o auth-service no respondieron (caidos, sin instancias en Eureka, timeout o 5xx,
 * incluido Kafka no disponible). Es el unico tipo de error que cuenta para el circuit breaker
 * "auditoria"; un 4xx (por ejemplo, evento rechazado por validacion) no lo abre.
 */
public class AuditoriaNoDisponibleException extends RuntimeException {

    public AuditoriaNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
