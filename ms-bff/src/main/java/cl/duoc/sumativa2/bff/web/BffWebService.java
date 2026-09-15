package cl.duoc.sumativa2.bff.web;

import java.util.List;

import org.springframework.stereotype.Service;

import cl.duoc.sumativa2.bff.clients.DataServiceClient;
import cl.duoc.sumativa2.bff.web.dtos.CuentaAnualWebDTO;
import cl.duoc.sumativa2.bff.web.dtos.InteresWebDTO;
import cl.duoc.sumativa2.bff.web.dtos.TransaccionWebDTO;

import lombok.RequiredArgsConstructor;

/**
 * Logica de presentacion exclusiva del canal Web: entrega la informacion
 * completa, porque el navegador tiene espacio en pantalla y ancho de banda
 * suficiente para mostrar todo el detalle.
 */
@Service
@RequiredArgsConstructor
public class BffWebService {

    private final DataServiceClient dataServiceClient;

    public List<CuentaAnualWebDTO> obtenerCuentasAnuales(String cuentaId) {
        return dataServiceClient.obtenerCuentasAnuales(cuentaId).stream()
                .map(c -> new CuentaAnualWebDTO(
                        c.id(), c.cuentaId(), c.fecha(), c.transaccion(), c.monto(), c.descripcion()))
                .toList();
    }

    public List<InteresWebDTO> obtenerIntereses(String cuentaId) {
        return dataServiceClient.obtenerIntereses(cuentaId).stream()
                .map(i -> new InteresWebDTO(
                        i.id(), i.cuentaId(), i.nombre(), i.saldo(), i.edad(), i.tipo()))
                .toList();
    }

    public List<TransaccionWebDTO> obtenerTransacciones(String id) {
        return dataServiceClient.obtenerTransacciones(id).stream()
                .map(t -> new TransaccionWebDTO(
                        t.registroId(), t.id(), t.fecha(), t.monto(), t.tipo()))
                .toList();
    }
}
