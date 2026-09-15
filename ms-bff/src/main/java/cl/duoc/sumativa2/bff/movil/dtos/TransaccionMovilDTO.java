package cl.duoc.sumativa2.bff.movil.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Canal Movil: de transacciones solo se envian fecha y monto. */
public record TransaccionMovilDTO(
        LocalDate fecha,
        BigDecimal monto) {
}
