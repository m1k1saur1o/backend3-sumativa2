package cl.duoc.sumativa2.bff.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce las excepciones a respuestas JSON homogeneas para los tres canales
 * (web, movil y cajeros), sin acoplarlos entre si.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DatosNoEncontradosException.class)
    public ResponseEntity<ApiErrorDTO> manejarNoEncontrado(DatosNoEncontradosException ex) {
        return construir(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ParametroInvalidoException.class)
    public ResponseEntity<ApiErrorDTO> manejarParametroInvalido(ParametroInvalidoException ex) {
        return construir(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ServicioDatosNoDisponibleException.class)
    public ResponseEntity<ApiErrorDTO> manejarServicioNoDisponible(ServicioDatosNoDisponibleException ex) {
        log.error("ms-backend no disponible", ex);
        return construir(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorDTO> manejarErrorInesperado(Exception ex) {
        log.error("Error inesperado en ms-bff", ex);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del BFF");
    }

    private ResponseEntity<ApiErrorDTO> construir(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status).body(new ApiErrorDTO(status.value(), mensaje));
    }
}
