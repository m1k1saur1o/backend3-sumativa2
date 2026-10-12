package cl.duoc.sumativa2.bff.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import cl.duoc.sumativa2.bff.auditoria.AuditoriaAtributos;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Traduce las excepciones a respuestas JSON homogeneas para los tres canales
 * (web, movil y cajeros), sin acoplarlos entre si. Cada error queda ademas registrado (codigo y
 * mensaje) para el evento de auditoria de la solicitud.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DatosNoEncontradosException.class)
    public ResponseEntity<ApiErrorDTO> manejarNoEncontrado(DatosNoEncontradosException ex,
                                                           HttpServletRequest request) {
        return construir(request, HttpStatus.NOT_FOUND, "DATOS_NO_ENCONTRADOS", ex.getMessage());
    }

    @ExceptionHandler(ParametroInvalidoException.class)
    public ResponseEntity<ApiErrorDTO> manejarParametroInvalido(ParametroInvalidoException ex,
                                                                HttpServletRequest request) {
        return construir(request, HttpStatus.BAD_REQUEST, "PARAMETRO_INVALIDO", ex.getMessage());
    }

    @ExceptionHandler(ServicioDatosNoDisponibleException.class)
    public ResponseEntity<ApiErrorDTO> manejarServicioNoDisponible(ServicioDatosNoDisponibleException ex,
                                                                   HttpServletRequest request) {
        log.error("ms-backend no disponible: {}", ex.getCause() != null ? ex.getCause().toString() : ex.getMessage());
        return construir(request, HttpStatus.SERVICE_UNAVAILABLE, "SERVICIO_DATOS_NO_DISPONIBLE", ex.getMessage());
    }

    /** Ruta inexistente bajo /api/** (antes caia en el manejador generico y respondia 500). */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorDTO> manejarRutaInexistente(NoResourceFoundException ex,
                                                              HttpServletRequest request) {
        return construir(request, HttpStatus.NOT_FOUND, "RUTA_NO_ENCONTRADA", "El recurso solicitado no existe");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorDTO> manejarMetodoNoSoportado(HttpRequestMethodNotSupportedException ex,
                                                                HttpServletRequest request) {
        return construir(request, HttpStatus.METHOD_NOT_ALLOWED, "METODO_NO_PERMITIDO",
                "Metodo " + ex.getMethod() + " no permitido; los endpoints del BFF son GET");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorDTO> manejarErrorInesperado(Exception ex, HttpServletRequest request) {
        log.error("Error inesperado en ms-bff", ex);
        return construir(request, HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO", "Error interno del BFF");
    }

    private ResponseEntity<ApiErrorDTO> construir(HttpServletRequest request, HttpStatus status, String codigo,
                                                  String mensaje) {
        AuditoriaAtributos.registrarError(request, codigo, mensaje);
        return ResponseEntity.status(status).body(new ApiErrorDTO(status.value(), mensaje));
    }
}
