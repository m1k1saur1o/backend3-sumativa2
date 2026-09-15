package cl.duoc.sumativa2.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.sumativa2.backend.dto.TransaccionDTO;
import cl.duoc.sumativa2.backend.entity.Transaccion;
import cl.duoc.sumativa2.backend.exception.RecursoNoEncontradoException;
import cl.duoc.sumativa2.backend.repository.TransaccionRepository;

import lombok.RequiredArgsConstructor;

/** Acceso a transacciones: consulta, ordena y traduce a DTO. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransaccionService {

    private final TransaccionRepository transaccionRepository;

    /** Registros del identificador, ordenados por fecha DESC, registro_id DESC. */
    public List<TransaccionDTO> buscarPorId(String idTransaccion) {
        String id = IdentificadorValidator.validar(idTransaccion, "id");

        List<Transaccion> registros = transaccionRepository.findByIdTransaccionOrderByFechaDescRegistroIdDesc(id);
        if (registros.isEmpty()) {
            throw new RecursoNoEncontradoException(
                    "No se encontraron transacciones para el id " + id);
        }
        return registros.stream().map(this::aDTO).toList();
    }

    private TransaccionDTO aDTO(Transaccion entidad) {
        return new TransaccionDTO(
                entidad.getRegistroId(),
                entidad.getIdTransaccion(),
                entidad.getFecha(),
                entidad.getMonto(),
                entidad.getTipo());
    }
}
