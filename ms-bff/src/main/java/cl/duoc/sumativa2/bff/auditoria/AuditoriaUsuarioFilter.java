package cl.duoc.sumativa2.bff.auditoria;

import java.io.IOException;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Se ejecuta dentro de la cadena de Spring Security, justo despues de validar el Bearer token, y
 * copia el usuario autenticado (claim sub) a un atributo del request. El filtro de auditoria, que
 * envuelve a Spring Security, lo lee al terminar la solicitud (cuando el SecurityContext ya se limpio).
 * Tambien cubre las respuestas 403: el usuario es valido aunque no tenga permisos.
 *
 * <p>No es un bean para que Spring Boot no lo registre ademas como filtro del contenedor.
 */
public class AuditoriaUsuarioFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            request.setAttribute(AuditoriaAtributos.USUARIO, auth.getName());
        }
        chain.doFilter(request, response);
    }
}
