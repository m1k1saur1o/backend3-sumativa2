package cl.duoc.sumativa2.bff.auditoria;

/**
 * Publica eventos de auditoria sin bloquear la solicitud HTTP. Las implementaciones no deben
 * lanzar excepciones: un fallo de auditoria nunca cambia la respuesta entregada al cliente.
 */
public interface AuditoriaPublisher {

    void publicar(BffCallEvent evento);
}
