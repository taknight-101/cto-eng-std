package com.platform.security.spring;

import com.platform.security.api.SecurityEvent;
import com.platform.security.api.SecurityEventDrill;
import com.platform.security.api.SecurityEventType;
import com.platform.security.api.SecurityLifecyclePhase;
import com.platform.security.api.SecurityMiddlewareOutcome;
import com.platform.security.api.SecurityPipeline;
import com.platform.security.api.SecurityRequestContext;
import com.platform.security.internal.CorrelationIds;
import com.platform.security.internal.SimpleJson;
import com.platform.security.spi.SecurityFailureHandler;
import com.platform.security.spi.SecurityFailureResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * The single Spring Security filter-chain integration point: builds the initial {@link
 * SecurityRequestContext}, runs the {@link SecurityPipeline}, and either continues the servlet
 * filter chain or writes a failure response - always clearing {@code SecurityContextHolder}
 * afterwards so state never leaks onto a pooled request-handling thread.
 */
public final class SecurityMiddlewareFilter extends OncePerRequestFilter {

    public static final String CONTEXT_REQUEST_ATTRIBUTE = SecurityMiddlewareFilter.class.getName() + ".CONTEXT";
    private static final String CORRELATION_HEADER = "X-Correlation-Id";

    private final SecurityPipeline pipeline;
    private final SecurityEventDrill eventDrill;
    private final SecurityFailureHandler failureHandler;

    public SecurityMiddlewareFilter(
            SecurityPipeline pipeline, SecurityEventDrill eventDrill, SecurityFailureHandler failureHandler) {
        this.pipeline = pipeline;
        this.eventDrill = eventDrill;
        this.failureHandler = failureHandler;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        SecurityRequestContext initialContext = buildInitialContext(request);
        request.setAttribute(CONTEXT_REQUEST_ATTRIBUTE, initialContext);
        try {
            SecurityMiddlewareOutcome outcome = pipeline.execute(initialContext);
            request.setAttribute(CONTEXT_REQUEST_ATTRIBUTE, outcome.context());
            if (outcome instanceof SecurityMiddlewareOutcome.Continue) {
                filterChain.doFilter(request, response);
                eventDrill.publish(SecurityEvent.of(SecurityEventType.PROCESSING_COMPLETED,
                        outcome.context().withPhase(SecurityLifecyclePhase.COMPLETED), "completion", Map.of()));
            } else if (outcome instanceof SecurityMiddlewareOutcome.ShortCircuit shortCircuit) {
                eventDrill.publish(SecurityEvent.of(SecurityEventType.REQUEST_REJECTED, shortCircuit.context(),
                        "rejection", Map.of("status", shortCircuit.status(), "reason", shortCircuit.reason())));
                writeFailure(response, shortCircuit);
            }
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void writeFailure(HttpServletResponse response, SecurityMiddlewareOutcome.ShortCircuit shortCircuit)
            throws IOException {
        SecurityFailureResponse failureResponse =
                failureHandler.handle(shortCircuit.context(), shortCircuit.status(), shortCircuit.reason());
        response.setStatus(failureResponse.status());
        response.setContentType("application/json");
        failureResponse.headers().forEach(response::setHeader);
        response.getWriter().write(SimpleJson.toJson(failureResponse.body()));
    }

    private SecurityRequestContext buildInitialContext(HttpServletRequest request) {
        SecurityRequestContext.Builder builder = SecurityRequestContext.builder()
                .correlationId(resolveCorrelationId(request))
                .requestMethod(request.getMethod())
                .requestPath(request.getRequestURI())
                .clientAddress(request.getRemoteAddr());
        for (String name : Collections.list(request.getHeaderNames())) {
            builder.header(name, request.getHeader(name));
        }
        return builder.build();
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        String header = request.getHeader(CORRELATION_HEADER);
        return (header != null && !header.isBlank()) ? header : CorrelationIds.newId();
    }
}
