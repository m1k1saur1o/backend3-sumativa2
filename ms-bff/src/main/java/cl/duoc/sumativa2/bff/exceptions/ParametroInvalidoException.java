package cl.duoc.sumativa2.bff.exceptions;

/** El identificador recibido (o rechazado por ms-backend) no es valido. */
public class ParametroInvalidoException extends RuntimeException {

    public ParametroInvalidoException(String mensaje) {
        super(mensaje);
    }
}
