package cl.duoc.sumativa2.backend.service;

import cl.duoc.sumativa2.backend.exception.ParametroInvalidoException;

/** Validacion comun de los identificadores recibidos por la API (VARCHAR(50) en la BD). */
final class IdentificadorValidator {

    private static final int LARGO_MAXIMO = 50;

    private IdentificadorValidator() {
    }

    static String validar(String valor, String nombreParametro) {
        if (valor == null || valor.isBlank()) {
            throw new ParametroInvalidoException("El parametro '" + nombreParametro + "' es obligatorio");
        }
        String limpio = valor.trim();
        if (limpio.length() > LARGO_MAXIMO) {
            throw new ParametroInvalidoException(
                    "El parametro '" + nombreParametro + "' no puede superar los " + LARGO_MAXIMO + " caracteres");
        }
        return limpio;
    }
}
