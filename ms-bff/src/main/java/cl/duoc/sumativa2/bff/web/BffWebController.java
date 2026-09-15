package cl.duoc.sumativa2.bff.web;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.sumativa2.bff.web.dtos.CuentaAnualWebDTO;
import cl.duoc.sumativa2.bff.web.dtos.InteresWebDTO;
import cl.duoc.sumativa2.bff.web.dtos.TransaccionWebDTO;

import lombok.RequiredArgsConstructor;

/** Endpoints exclusivos del canal Web. */
@RestController
@RequestMapping(value = "/api/web", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class BffWebController {

    private final BffWebService bffWebService;

    @GetMapping("/cuentas/{cuentaId}")
    public List<CuentaAnualWebDTO> obtenerCuentas(@PathVariable String cuentaId) {
        return bffWebService.obtenerCuentasAnuales(cuentaId);
    }

    @GetMapping("/intereses/{cuentaId}")
    public List<InteresWebDTO> obtenerIntereses(@PathVariable String cuentaId) {
        return bffWebService.obtenerIntereses(cuentaId);
    }

    @GetMapping("/transacciones/{id}")
    public List<TransaccionWebDTO> obtenerTransacciones(@PathVariable String id) {
        return bffWebService.obtenerTransacciones(id);
    }
}
