package cl.duoc.sumativa2.bff.cajeros.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Canal Cajeros: del ultimo movimiento solo se muestran fecha y monto. */
public record CuentaAnualCajeroDTO(
        LocalDate fecha,
        BigDecimal monto) {
}
