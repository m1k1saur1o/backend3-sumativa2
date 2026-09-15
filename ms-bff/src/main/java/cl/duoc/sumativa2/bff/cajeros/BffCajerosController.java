package cl.duoc.sumativa2.bff.cajeros;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.sumativa2.bff.cajeros.dtos.CuentaAnualCajeroDTO;
import cl.duoc.sumativa2.bff.cajeros.dtos.InteresCajeroDTO;
import cl.duoc.sumativa2.bff.cajeros.dtos.TransaccionCajeroDTO;

import lombok.RequiredArgsConstructor;

/** Endpoints exclusivos del canal Cajeros Automaticos. */
@RestController
@RequestMapping(value = "/api/cajeros", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class BffCajerosController {

    private final BffCajerosService bffCajerosService;

    @GetMapping("/cuentas/{cuentaId}")
    public CuentaAnualCajeroDTO obtenerCuenta(@PathVariable String cuentaId) {
        return bffCajerosService.obtenerUltimaCuentaAnual(cuentaId);
    }

    @GetMapping("/intereses/{cuentaId}")
    public InteresCajeroDTO obtenerInteres(@PathVariable String cuentaId) {
        return bffCajerosService.obtenerUltimoInteres(cuentaId);
    }

    @GetMapping("/transacciones/{id}")
    public TransaccionCajeroDTO obtenerTransaccion(@PathVariable String id) {
        return bffCajerosService.obtenerUltimaTransaccion(id);
    }
}
