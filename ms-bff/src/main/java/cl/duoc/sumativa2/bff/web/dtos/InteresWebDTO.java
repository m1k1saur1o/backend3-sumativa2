package cl.duoc.sumativa2.bff.web.dtos;

import java.math.BigDecimal;

/** Canal Web: intereses con todos sus campos. */
public record InteresWebDTO(
        Long id,
        String cuentaId,
        String nombre,
        BigDecimal saldo,
        Integer edad,
        String tipo) {
}
