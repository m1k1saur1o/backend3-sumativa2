package cl.duoc.sumativa2.bff.dtos.backend;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Espejo del contrato JSON de ms-backend para cuentas_anuales. */
public record CuentaAnualBackendDTO(
        Long id,
        String cuentaId,
        LocalDate fecha,
        String transaccion,
        BigDecimal monto,
        String descripcion) {
}
