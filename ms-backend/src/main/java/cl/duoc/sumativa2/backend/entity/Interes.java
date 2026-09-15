package cl.duoc.sumativa2.backend.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

/** Mapeo de la tabla intereses (solo lectura). */
@Entity
@Table(name = "intereses")
@Getter
@Setter
public class Interes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "cuenta_id", nullable = false, length = 50)
    private String cuentaId;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "saldo", nullable = false, precision = 15, scale = 2)
    private BigDecimal saldo;

    @Column(name = "edad", nullable = false)
    private Integer edad;

    @Column(name = "tipo", nullable = false, length = 50)
    private String tipo;
}
