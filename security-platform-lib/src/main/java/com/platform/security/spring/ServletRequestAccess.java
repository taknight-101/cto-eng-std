package com.platform.security.spring;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

/**
 * Explicit, clearly-named escape hatch to the underlying servlet request/response for advanced
 * users. Not part of {@link com.platform.security.api.SecurityRequestContext} itself.
 *
 * <p>Only reliable once Spring's {@code DispatcherServlet} has bound the current request (i.e.
 * from controller/service code), not from within {@link SecurityMiddlewareFilter} itself, which
 * already has direct access to the servlet request/response.
 */
public final class ServletRequestAccess {

    private ServletRequestAccess() {
    }

    public static Optional<HttpServletRequest> currentRequest() {
        return currentAttributes().map(ServletRequestAttributes::getRequest);
    }

    public static Optional<HttpServletResponse> currentResponse() {
        return currentAttributes().map(ServletRequestAttributes::getResponse);
    }

    private static Optional<ServletRequestAttributes> currentAttributes() {
        var attributes = RequestContextHolder.getRequestAttributes();
        return attributes instanceof ServletRequestAttributes sra ? Optional.of(sra) : Optional.empty();
    }
}
