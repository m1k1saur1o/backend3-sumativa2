package cl.duoc.sumativa2.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.duoc.sumativa2.backend.entity.CuentaAnual;

public interface CuentaAnualRepository extends JpaRepository<CuentaAnual, Long> {

    /** Criterio deterministico acordado: ORDER BY fecha DESC, id DESC. */
    List<CuentaAnual> findByCuentaIdOrderByFechaDescIdDesc(String cuentaId);
}
