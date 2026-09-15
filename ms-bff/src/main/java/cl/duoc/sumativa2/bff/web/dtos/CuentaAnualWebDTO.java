package cl.duoc.sumativa2.bff.web.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Canal Web: cuentas_anuales con todos sus campos. */
public record CuentaAnualWebDTO(
        Long id,
        String cuentaId,
        LocalDate fecha,
        String transaccion,
        BigDecimal monto,
        String descripcion) {
}
