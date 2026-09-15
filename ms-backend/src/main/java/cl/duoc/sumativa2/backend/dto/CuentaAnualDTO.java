package cl.duoc.sumativa2.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Contrato REST de cuentas_anuales (no se expone la entidad JPA). */
public record CuentaAnualDTO(
        Long id,
        String cuentaId,
        LocalDate fecha,
        String transaccion,
        BigDecimal monto,
        String descripcion) {
}
