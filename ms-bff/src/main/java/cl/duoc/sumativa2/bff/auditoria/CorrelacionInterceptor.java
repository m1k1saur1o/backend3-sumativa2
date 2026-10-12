package cl.duoc.sumativa2.bff.auditoria;

import java.io.IOException;

import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * Propaga el requestId de la solicitud en curso (MDC) a las llamadas salientes hacia ms-backend,
 * para poder correlacionar logs de ambos servicios.
 */
public class CorrelacionInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        String requestId = MDC.get(AuditoriaAtributos.MDC_REQUEST_ID);
        if (requestId != null && !request.getHeaders().containsKey(AuditoriaAtributos.HEADER_REQUEST_ID)) {
            request.getHeaders().set(AuditoriaAtributos.HEADER_REQUEST_ID, requestId);
        }
        return execution.execute(request, body);
    }
}
