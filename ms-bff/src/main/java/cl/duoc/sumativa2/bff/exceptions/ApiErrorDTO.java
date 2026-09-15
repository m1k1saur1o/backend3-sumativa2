package cl.duoc.sumativa2.bff.exceptions;

/** Cuerpo JSON unico para todas las respuestas de error del BFF. */
public record ApiErrorDTO(
        int status,
        String message) {
}
