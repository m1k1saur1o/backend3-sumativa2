package cl.duoc.sumativa2.backend.dto;

/** Cuerpo JSON unico para todas las respuestas de error del microservicio. */
public record ApiErrorDTO(
        int status,
        String message) {
}
