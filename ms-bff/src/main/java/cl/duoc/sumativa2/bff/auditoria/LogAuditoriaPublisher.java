package cl.duoc.sumativa2.bff.auditoria;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementacion usada con AUDIT_ENABLED=false (por ejemplo, ms-bff ejecutado sin el resto de la
 * plataforma): el evento solo queda en el log de la aplicacion.
 */
public class LogAuditoriaPublisher implements AuditoriaPublisher {

    private static final Logger log = LoggerFactory.getLogger(LogAuditoriaPublisher.class);

    @Override
    public void publicar(BffCallEvent evento) {
        log.info("Auditoria (solo log) {}", evento);
    }
}
