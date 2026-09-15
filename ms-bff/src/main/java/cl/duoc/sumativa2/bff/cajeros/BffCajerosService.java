package cl.duoc.sumativa2.bff.cajeros;

import org.springframework.stereotype.Service;

import cl.duoc.sumativa2.bff.cajeros.dtos.CuentaAnualCajeroDTO;
import cl.duoc.sumativa2.bff.cajeros.dtos.InteresCajeroDTO;
import cl.duoc.sumativa2.bff.cajeros.dtos.TransaccionCajeroDTO;
import cl.duoc.sumativa2.bff.clients.DataServiceClient;

import lombok.RequiredArgsConstructor;

/**
 * Logica de presentacion exclusiva del canal Cajeros Automaticos: la pantalla
 * del cajero es minima, asi que se entrega un unico registro (el ultimo) con
 * los campos estrictamente necesarios.
 *
 * <p>ms-backend entrega las listas ya ordenadas de forma deterministica
 * (cuentas_anuales: fecha DESC, id DESC; intereses: id DESC; transacciones:
 * fecha DESC, registro_id DESC), por lo que el ultimo registro es siempre el
 * primer elemento de la lista. Si no hay registros, el cliente HTTP ya lanzo
 * la excepcion 404 correspondiente.
 */
@Service
@RequiredArgsConstructor
public class BffCajerosService {

    private final DataServiceClient dataServiceClient;

    public CuentaAnualCajeroDTO obtenerUltimaCuentaAnual(String cuentaId) {
        var ultimo = dataServiceClient.obtenerCuentasAnuales(cuentaId).get(0);
        return new CuentaAnualCajeroDTO(ultimo.fecha(), ultimo.monto());
    }

    public InteresCajeroDTO obtenerUltimoInteres(String cuentaId) {
        var ultimo = dataServiceClient.obtenerIntereses(cuentaId).get(0);
        return new InteresCajeroDTO(ultimo.nombre(), ultimo.saldo());
    }

    public TransaccionCajeroDTO obtenerUltimaTransaccion(String id) {
        var ultima = dataServiceClient.obtenerTransacciones(id).get(0);
        return new TransaccionCajeroDTO(ultima.fecha(), ultima.monto());
    }
}
