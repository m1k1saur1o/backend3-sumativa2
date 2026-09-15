package cl.duoc.sumativa2.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.duoc.sumativa2.backend.entity.Transaccion;

public interface TransaccionRepository extends JpaRepository<Transaccion, Long> {

    /** Criterio deterministico acordado: ORDER BY fecha DESC, registro_id DESC. */
    List<Transaccion> findByIdTransaccionOrderByFechaDescRegistroIdDesc(String idTransaccion);
}
