package cl.duoc.sumativa2.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.sumativa2.backend.dto.InteresDTO;
import cl.duoc.sumativa2.backend.entity.Interes;
import cl.duoc.sumativa2.backend.exception.RecursoNoEncontradoException;
import cl.duoc.sumativa2.backend.repository.InteresRepository;

import lombok.RequiredArgsConstructor;

/** Acceso a intereses: consulta, ordena y traduce a DTO. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InteresService {

    private final InteresRepository interesRepository;

    /** Registros de la cuenta, ordenados por id DESC (ultimo ingresado primero). */
    public List<InteresDTO> buscarPorCuentaId(String cuentaId) {
        String id = IdentificadorValidator.validar(cuentaId, "cuentaId");

        List<Interes> registros = interesRepository.findByCuentaIdOrderByIdDesc(id);
        if (registros.isEmpty()) {
            throw new RecursoNoEncontradoException(
                    "No se encontraron registros de intereses para la cuenta " + id);
        }
        return registros.stream().map(this::aDTO).toList();
    }

    private InteresDTO aDTO(Interes entidad) {
        return new InteresDTO(
                entidad.getId(),
                entidad.getCuentaId(),
                entidad.getNombre(),
                entidad.getSaldo(),
                entidad.getEdad(),
                entidad.getTipo());
    }
}
