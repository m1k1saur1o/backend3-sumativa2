package cl.duoc.sumativa2.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Contrato REST de transacciones (no se expone la entidad JPA). */
public record TransaccionDTO(
        Long registroId,
        String id,
        LocalDate fecha,
        BigDecimal monto,
        String tipo) {
}
