package cl.duoc.sumativa2.bff.exceptions;

/** ms-backend no respondio (caido, timeout o error 5xx). */
public class ServicioDatosNoDisponibleException extends RuntimeException {

    public ServicioDatosNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
