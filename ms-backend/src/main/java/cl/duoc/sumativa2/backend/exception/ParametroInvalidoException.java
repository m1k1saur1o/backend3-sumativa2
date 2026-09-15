package cl.duoc.sumativa2.backend.exception;

/** Se lanza cuando el identificador recibido no cumple el formato esperado. */
public class ParametroInvalidoException extends RuntimeException {

    public ParametroInvalidoException(String mensaje) {
        super(mensaje);
    }
}
