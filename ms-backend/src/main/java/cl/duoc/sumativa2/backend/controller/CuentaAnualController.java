package cl.duoc.sumativa2.backend.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.sumativa2.backend.dto.CuentaAnualDTO;
import cl.duoc.sumativa2.backend.service.CuentaAnualService;

import lombok.RequiredArgsConstructor;

/** API REST de cuentas_anuales, consumida por ms-bff. */
@RestController
@RequestMapping(value = "/api/cuentas-anuales", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class CuentaAnualController {

    private final CuentaAnualService cuentaAnualService;

    @GetMapping("/{cuentaId}")
    public List<CuentaAnualDTO> buscarPorCuentaId(@PathVariable String cuentaId) {
        return cuentaAnualService.buscarPorCuentaId(cuentaId);
    }
}
