package cl.duoc.sumativa2.bff.movil;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.sumativa2.bff.movil.dtos.CuentaAnualMovilDTO;
import cl.duoc.sumativa2.bff.movil.dtos.InteresMovilDTO;
import cl.duoc.sumativa2.bff.movil.dtos.TransaccionMovilDTO;

import lombok.RequiredArgsConstructor;

/** Endpoints exclusivos del canal Movil. */
@RestController
@RequestMapping(value = "/api/movil", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class BffMovilController {

    private final BffMovilService bffMovilService;

    @GetMapping("/cuentas/{cuentaId}")
    public List<CuentaAnualMovilDTO> obtenerCuentas(@PathVariable String cuentaId) {
        return bffMovilService.obtenerCuentasAnuales(cuentaId);
    }

    @GetMapping("/intereses/{cuentaId}")
    public List<InteresMovilDTO> obtenerIntereses(@PathVariable String cuentaId) {
        return bffMovilService.obtenerIntereses(cuentaId);
    }

    @GetMapping("/transacciones/{id}")
    public List<TransaccionMovilDTO> obtenerTransacciones(@PathVariable String id) {
        return bffMovilService.obtenerTransacciones(id);
    }
}
