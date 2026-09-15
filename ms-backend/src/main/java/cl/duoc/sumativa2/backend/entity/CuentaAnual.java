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

/** Mapeo de la tabla cuentas_anuales (solo lectura). */
@Entity
@Table(name = "cuentas_anuales")
@Getter
@Setter
public class CuentaAnual {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "cuenta_id", nullable = false, length = 50)
    private String cuentaId;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "transaccion", nullable = false, length = 50)
    private String transaccion;

    @Column(name = "monto", nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    @Column(name = "descripcion", length = 255)
    private String descripcion;
}
