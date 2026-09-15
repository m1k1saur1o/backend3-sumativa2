package cl.duoc.sumativa2.backend.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.sumativa2.backend.dto.InteresDTO;
import cl.duoc.sumativa2.backend.service.InteresService;

import lombok.RequiredArgsConstructor;

/** API REST de intereses, consumida por ms-bff. */
@RestController
@RequestMapping(value = "/api/intereses", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class InteresController {

    private final InteresService interesService;

    @GetMapping("/{cuentaId}")
    public List<InteresDTO> buscarPorCuentaId(@PathVariable String cuentaId) {
        return interesService.buscarPorCuentaId(cuentaId);
    }
}
