package cl.duoc.sumativa2.bff.exceptions;

/** ms-backend respondio 404: no hay registros para el identificador consultado. */
public class DatosNoEncontradosException extends RuntimeException {

    public DatosNoEncontradosException(String mensaje) {
        super(mensaje);
    }
}
