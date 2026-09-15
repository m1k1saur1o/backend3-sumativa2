package cl.duoc.sumativa2.bff.movil.dtos;

import java.math.BigDecimal;

/** Canal Movil: de intereses solo se envian nombre, saldo y tipo (sin edad). */
public record InteresMovilDTO(
        String nombre,
        BigDecimal saldo,
        String tipo) {
}
