package cl.duoc.sumativa2.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.sumativa2.backend.dto.CuentaAnualDTO;
import cl.duoc.sumativa2.backend.entity.CuentaAnual;
import cl.duoc.sumativa2.backend.exception.RecursoNoEncontradoException;
import cl.duoc.sumativa2.backend.repository.CuentaAnualRepository;

import lombok.RequiredArgsConstructor;

/** Acceso a cuentas_anuales: consulta, ordena y traduce a DTO. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CuentaAnualService {

    private final CuentaAnualRepository cuentaAnualRepository;

    /** Registros de la cuenta, ordenados por fecha DESC, id DESC (mas reciente primero). */
    public List<CuentaAnualDTO> buscarPorCuentaId(String cuentaId) {
        String id = IdentificadorValidator.validar(cuentaId, "cuentaId");

        List<CuentaAnual> registros = cuentaAnualRepository.findByCuentaIdOrderByFechaDescIdDesc(id);
        if (registros.isEmpty()) {
            throw new RecursoNoEncontradoException(
                    "No se encontraron registros de cuentas anuales para la cuenta " + id);
        }
        return registros.stream().map(this::aDTO).toList();
    }

    private CuentaAnualDTO aDTO(CuentaAnual entidad) {
        return new CuentaAnualDTO(
                entidad.getId(),
                entidad.getCuentaId(),
                entidad.getFecha(),
                entidad.getTransaccion(),
                entidad.getMonto(),
                entidad.getDescripcion());
    }
}
