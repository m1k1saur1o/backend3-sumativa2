package cl.duoc.sumativa2.bff.support;

import cl.duoc.sumativa2.bff.support.ServidorHttpFalso.Respuesta;
import cl.duoc.sumativa2.bff.support.ServidorHttpFalso.Solicitud;

/**
 * Respuestas con el mismo contrato que ms-backend: datos para la cuenta/id 110, 404 para el
 * resto y 400 si el identificador supera los 50 caracteres.
 */
public final class MsBackendFalso {

    private MsBackendFalso() {
    }

    public static Respuesta responder(Solicitud s) {
        String id = s.ruta().substring(s.ruta().lastIndexOf('/') + 1);
        if (id.length() > 50) {
            return new Respuesta(400, "{\"status\":400,\"message\":\"El parametro no puede superar los 50 caracteres\"}");
        }
        if (!"110".equals(id)) {
            return new Respuesta(404, "{\"status\":404,\"message\":\"No se encontraron registros\"}");
        }
        if (s.ruta().startsWith("/api/cuentas-anuales/")) {
            return new Respuesta(200, """
                    [{"id":2,"cuentaId":"110","fecha":"2024-12-22","transaccion":"deposito","monto":2500.00,"descripcion":"Deposito"},
                     {"id":1,"cuentaId":"110","fecha":"2024-11-23","transaccion":"compra","monto":1500.00,"descripcion":"Ingreso mensual"}]
                    """);
        }
        if (s.ruta().startsWith("/api/intereses/")) {
            return new Respuesta(200, """
                    [{"id":646,"cuentaId":"110","nombre":"Charlie Green","saldo":10000.00,"edad":25,"tipo":"prestamo"}]
                    """);
        }
        if (s.ruta().startsWith("/api/transacciones/")) {
            return new Respuesta(200, """
                    [{"registroId":240,"id":"110","fecha":"2024-07-05","monto":800.00,"tipo":"debito"}]
                    """);
        }
        return new Respuesta(404, "{\"status\":404,\"message\":\"Ruta inexistente\"}");
    }
}
