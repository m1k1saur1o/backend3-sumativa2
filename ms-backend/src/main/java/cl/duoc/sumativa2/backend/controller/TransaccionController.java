package cl.duoc.sumativa2.backend.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.sumativa2.backend.dto.TransaccionDTO;
import cl.duoc.sumativa2.backend.service.TransaccionService;

import lombok.RequiredArgsConstructor;

/** API REST de transacciones, consumida por ms-bff. */
@RestController
@RequestMapping(value = "/api/transacciones", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class TransaccionController {

    private final TransaccionService transaccionService;

    @GetMapping("/{id}")
    public List<TransaccionDTO> buscarPorId(@PathVariable String id) {
        return transaccionService.buscarPorId(id);
    }
}
