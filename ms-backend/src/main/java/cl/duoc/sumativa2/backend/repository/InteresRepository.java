package cl.duoc.sumativa2.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.duoc.sumativa2.backend.entity.Interes;

public interface InteresRepository extends JpaRepository<Interes, Long> {

    /** Criterio deterministico acordado: ORDER BY id DESC. */
    List<Interes> findByCuentaIdOrderByIdDesc(String cuentaId);
}
