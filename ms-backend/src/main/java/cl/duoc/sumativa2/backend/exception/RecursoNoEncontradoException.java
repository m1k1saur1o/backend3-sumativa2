package cl.duoc.sumativa2.backend.exception;

/** Se lanza cuando no existe ningun registro para el identificador consultado. */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
