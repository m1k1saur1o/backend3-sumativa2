package cl.duoc.sumativa2.backend.dto;

import java.math.BigDecimal;

/** Contrato REST de intereses (no se expone la entidad JPA). */
public record InteresDTO(
        Long id,
        String cuentaId,
        String nombre,
        BigDecimal saldo,
        Integer edad,
        String tipo) {
}
