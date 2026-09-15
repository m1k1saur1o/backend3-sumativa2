package cl.duoc.sumativa2.bff.movil;

import java.util.List;

import org.springframework.stereotype.Service;

import cl.duoc.sumativa2.bff.clients.DataServiceClient;
import cl.duoc.sumativa2.bff.movil.dtos.CuentaAnualMovilDTO;
import cl.duoc.sumativa2.bff.movil.dtos.InteresMovilDTO;
import cl.duoc.sumativa2.bff.movil.dtos.TransaccionMovilDTO;

import lombok.RequiredArgsConstructor;

/**
 * Logica de presentacion exclusiva del canal Movil: recorta campos y cantidad
 * de registros para reducir el consumo de datos y el tamano de la respuesta.
 *
 * <p>Criterio de seleccion en cuentas_anuales: los 3 registros mas recientes.
 * ms-backend ya entrega la lista ordenada por fecha DESC, id DESC, de modo que
 * basta con tomar los 3 primeros elementos.
 */
@Service
@RequiredArgsConstructor
public class BffMovilService {

    private static final int MAX_REGISTROS_CUENTAS = 3;

    private final DataServiceClient dataServiceClient;

    public List<CuentaAnualMovilDTO> obtenerCuentasAnuales(String cuentaId) {
        return dataServiceClient.obtenerCuentasAnuales(cuentaId).stream()
                .limit(MAX_REGISTROS_CUENTAS)
                .map(c -> new CuentaAnualMovilDTO(c.fecha(), c.monto(), c.transaccion()))
                .toList();
    }

    public List<InteresMovilDTO> obtenerIntereses(String cuentaId) {
        return dataServiceClient.obtenerIntereses(cuentaId).stream()
                .map(i -> new InteresMovilDTO(i.nombre(), i.saldo(), i.tipo()))
                .toList();
    }

    public List<TransaccionMovilDTO> obtenerTransacciones(String id) {
        return dataServiceClient.obtenerTransacciones(id).stream()
                .map(t -> new TransaccionMovilDTO(t.fecha(), t.monto()))
                .toList();
    }
}
