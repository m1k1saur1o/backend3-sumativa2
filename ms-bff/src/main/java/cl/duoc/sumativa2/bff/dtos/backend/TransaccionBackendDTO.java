package cl.duoc.sumativa2.bff.dtos.backend;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Espejo del contrato JSON de ms-backend para transacciones. */
public record TransaccionBackendDTO(
        Long registroId,
        String id,
        LocalDate fecha,
        BigDecimal monto,
        String tipo) {
}
