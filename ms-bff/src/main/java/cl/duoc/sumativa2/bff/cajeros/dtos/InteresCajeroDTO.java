package cl.duoc.sumativa2.bff.cajeros.dtos;

import java.math.BigDecimal;

/** Canal Cajeros: del ultimo registro de intereses solo nombre y saldo. */
public record InteresCajeroDTO(
        String nombre,
        BigDecimal saldo) {
}
