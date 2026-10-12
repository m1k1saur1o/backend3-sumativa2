package cl.duoc.sumativa2.bff.auditoria;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Atributos de request y cabeceras que comparten el filtro de auditoria, la seguridad y el
 * manejador de errores para armar el evento sin acoplarse entre si.
 */
public final class AuditoriaAtributos {

    /** Cabecera de correlacion: se acepta del cliente si es valida, se genera si no, y se devuelve. */
    public static final String HEADER_REQUEST_ID = "X-Request-Id";
    /** Clave del MDC (logs) y atributo con el requestId de la solicitud en curso. */
    public static final String MDC_REQUEST_ID = "requestId";

    static final String USUARIO = AuditoriaAtributos.class.getName() + ".usuario";
    static final String ERROR_CODIGO = AuditoriaAtributos.class.getName() + ".errorCodigo";
    static final String ERROR_MENSAJE = AuditoriaAtributos.class.getName() + ".errorMensaje";

    private AuditoriaAtributos() {
    }

    /** Registra el error funcional o de seguridad que se informara en el evento de auditoria. */
    public static void registrarError(HttpServletRequest request, String codigo, String mensaje) {
        request.setAttribute(ERROR_CODIGO, codigo);
        request.setAttribute(ERROR_MENSAJE, mensaje);
    }
}
