package cl.duoc.sumativa2.bff.web.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Canal Web: transacciones con todos sus campos. */
public record TransaccionWebDTO(
        Long registroId,
        String id,
        LocalDate fecha,
        BigDecimal monto,
        String tipo) {
}
