package cl.duoc.sumativa2.backend.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import cl.duoc.sumativa2.backend.dto.ApiErrorDTO;

/** Traduce las excepciones del microservicio a respuestas JSON homogeneas. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ApiErrorDTO> manejarNoEncontrado(RecursoNoEncontradoException ex) {
        return construir(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ParametroInvalidoException.class)
    public ResponseEntity<ApiErrorDTO> manejarParametroInvalido(ParametroInvalidoException ex) {
        return construir(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorDTO> manejarErrorInesperado(Exception ex) {
        log.error("Error inesperado en ms-backend", ex);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del microservicio de datos");
    }

    private ResponseEntity<ApiErrorDTO> construir(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status).body(new ApiErrorDTO(status.value(), mensaje));
    }
}
