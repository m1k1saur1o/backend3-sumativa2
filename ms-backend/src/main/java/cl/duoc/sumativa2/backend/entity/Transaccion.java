package cl.duoc.sumativa2.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

/**
 * Mapeo de la tabla transacciones (solo lectura).
 *
 * <p>Ojo: la clave primaria de esta tabla es {@code registro_id}; la columna
 * {@code id} es el identificador de negocio (VARCHAR) por el que se consulta,
 * y se mapea al atributo {@code idTransaccion} para evitar ambiguedades.
 */
@Entity
@Table(name = "transacciones")
@Getter
@Setter
public class Transaccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "registro_id")
    private Long registroId;

    @Column(name = "id", nullable = false, length = 50)
    private String idTransaccion;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "monto", nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    @Column(name = "tipo", nullable = false, length = 50)
    private String tipo;
}
