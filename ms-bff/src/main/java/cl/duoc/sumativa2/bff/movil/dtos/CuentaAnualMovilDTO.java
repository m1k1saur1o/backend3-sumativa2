package cl.duoc.sumativa2.bff.movil.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Canal Movil: de cuentas_anuales solo se envian fecha, monto y transaccion. */
public record CuentaAnualMovilDTO(
        LocalDate fecha,
        BigDecimal monto,
        String transaccion) {
}
