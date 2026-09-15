package cl.duoc.sumativa2.bff.clients;

import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import cl.duoc.sumativa2.bff.dtos.backend.CuentaAnualBackendDTO;
import cl.duoc.sumativa2.bff.dtos.backend.InteresBackendDTO;
import cl.duoc.sumativa2.bff.dtos.backend.TransaccionBackendDTO;
import cl.duoc.sumativa2.bff.exceptions.DatosNoEncontradosException;
import cl.duoc.sumativa2.bff.exceptions.ParametroInvalidoException;
import cl.duoc.sumativa2.bff.exceptions.ServicioDatosNoDisponibleException;

import lombok.RequiredArgsConstructor;

/**
 * Unico punto de entrada hacia ms-backend: conoce sus rutas y traduce sus
 * codigos HTTP a excepciones del BFF. Los tres canales (web, movil y cajeros)
 * lo reutilizan sin acoplarse entre si, porque devuelve siempre los datos
 * crudos del backend y no aplica ninguna logica de presentacion.
 *
 * <p>El backend entrega las listas ya ordenadas de mas reciente a mas antiguo
 * (cuentas_anuales: fecha DESC, id DESC; intereses: id DESC; transacciones:
 * fecha DESC, registro_id DESC), por lo que "los N ultimos registros" son
 * siempre los N primeros elementos de la lista.
 */
@Component
@RequiredArgsConstructor
public class DataServiceClient {

    private static final ParameterizedTypeReference<List<CuentaAnualBackendDTO>> TIPO_CUENTAS =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<List<InteresBackendDTO>> TIPO_INTERESES =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<List<TransaccionBackendDTO>> TIPO_TRANSACCIONES =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient dataServiceRestClient;

    public List<CuentaAnualBackendDTO> obtenerCuentasAnuales(String cuentaId) {
        return obtenerLista("/api/cuentas-anuales/{cuentaId}", cuentaId, TIPO_CUENTAS,
                "No se encontraron registros de cuentas anuales para la cuenta " + cuentaId);
    }

    public List<InteresBackendDTO> obtenerIntereses(String cuentaId) {
        return obtenerLista("/api/intereses/{cuentaId}", cuentaId, TIPO_INTERESES,
                "No se encontraron registros de intereses para la cuenta " + cuentaId);
    }

    public List<TransaccionBackendDTO> obtenerTransacciones(String id) {
        return obtenerLista("/api/transacciones/{id}", id, TIPO_TRANSACCIONES,
                "No se encontraron transacciones para el id " + id);
    }

    private <T> List<T> obtenerLista(String uri, String identificador,
                                     ParameterizedTypeReference<List<T>> tipo, String mensajeNoEncontrado) {
        if (identificador == null || identificador.isBlank()) {
            throw new ParametroInvalidoException("El identificador consultado es obligatorio");
        }

        List<T> respuesta;
        try {
            respuesta = dataServiceRestClient.get()
                    .uri(uri, identificador)
                    .retrieve()
                    .body(tipo);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new DatosNoEncontradosException(mensajeNoEncontrado);
        } catch (HttpClientErrorException.BadRequest ex) {
            throw new ParametroInvalidoException("El identificador '" + identificador + "' no es valido");
        } catch (ResourceAccessException | HttpServerErrorException ex) {
            throw new ServicioDatosNoDisponibleException(
                    "El microservicio de datos no esta disponible en este momento", ex);
        }

        if (respuesta == null || respuesta.isEmpty()) {
            throw new DatosNoEncontradosException(mensajeNoEncontrado);
        }
        return respuesta;
    }
}
