package cl.duoc.sumativa2.bff.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import cl.duoc.sumativa2.bff.auditoria.AuditoriaAtributos;
import cl.duoc.sumativa2.bff.exceptions.ApiErrorDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Mantiene el comportamiento estandar de RFC 6750 (cabecera WWW-Authenticate y codigos 401/403)
 * con el mismo cuerpo JSON que el resto de errores del BFF ({@link ApiErrorDTO}). Los mensajes
 * son genericos: nunca incluyen el token ni el motivo exacto del rechazo.
 */
@Component
public class JsonSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final BearerTokenAuthenticationEntryPoint entryPoint = new BearerTokenAuthenticationEntryPoint();
    private final BearerTokenAccessDeniedHandler deniedHandler = new BearerTokenAccessDeniedHandler();
    private final ObjectMapper objectMapper;

    public JsonSecurityErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        entryPoint.commence(request, response, authException);

        boolean sinToken = request.getHeader("Authorization") == null;
        String mensaje = sinToken
                ? "Falta el token. Envie Authorization: Bearer <access_token>"
                : "Token invalido, expirado o no emitido para este servicio";
        AuditoriaAtributos.registrarError(request, sinToken ? "TOKEN_AUSENTE" : "TOKEN_INVALIDO", mensaje);
        write(response, HttpStatus.UNAUTHORIZED, mensaje);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        deniedHandler.handle(request, response, accessDeniedException);

        String mensaje = "El token no tiene los roles necesarios (USER o ADMIN)";
        AuditoriaAtributos.registrarError(request, "ACCESO_DENEGADO", mensaje);
        write(response, HttpStatus.FORBIDDEN, mensaje);
    }

    private void write(HttpServletResponse response, HttpStatus status, String mensaje) throws IOException {

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), new ApiErrorDTO(status.value(), mensaje));
    }
}
