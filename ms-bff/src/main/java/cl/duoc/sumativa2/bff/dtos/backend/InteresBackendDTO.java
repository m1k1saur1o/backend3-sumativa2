package cl.duoc.sumativa2.bff.dtos.backend;

import java.math.BigDecimal;

/** Espejo del contrato JSON de ms-backend para intereses. */
public record InteresBackendDTO(
        Long id,
        String cuentaId,
        String nombre,
        BigDecimal saldo,
        Integer edad,
        String tipo) {
}
