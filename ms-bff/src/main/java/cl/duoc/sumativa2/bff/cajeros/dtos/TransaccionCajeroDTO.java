package cl.duoc.sumativa2.bff.cajeros.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Canal Cajeros: de la ultima transaccion solo fecha y monto. */
public record TransaccionCajeroDTO(
        LocalDate fecha,
        BigDecimal monto) {
}
