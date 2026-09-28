package com.platform.security.spring;

import com.platform.security.api.SecurityMiddlewareRegistry;
import com.platform.security.api.SecurityPipeline;
import com.platform.security.authorization.AuthorizationMiddleware;
import com.platform.security.authorization.AuthorizationPolicies;
import com.platform.security.events.DefaultSecurityEventDrill;
import com.platform.security.pipeline.DefaultSecurityMiddlewareRegistry;
import com.platform.security.pipeline.DefaultSecurityPipeline;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Exercises {@link SecurityMiddlewareFilter} end-to-end against real servlet mock objects and a
 * real {@link DefaultSecurityPipeline}/{@link DefaultSecurityMiddlewareRegistry}, standing in
 * for full Spring Security filter-chain integration without needing a full application context.
 */
class SecurityMiddlewareFilterIntegrationTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requestWithoutAuthorityIsRejectedBeforeReachingDownstreamFilterChain() throws Exception {
        SecurityMiddlewareRegistry registry = new DefaultSecurityMiddlewareRegistry();
        DefaultSecurityEventDrill eventDrill = new DefaultSecurityEventDrill();
        registry.register("authorization", new AuthorizationMiddleware(
                AuthorizationPolicies.requireAuthority("ROLE_ADMIN"), eventDrill));
        SecurityPipeline pipeline = new DefaultSecurityPipeline(registry);
        SecurityMiddlewareFilter filter =
                new SecurityMiddlewareFilter(pipeline, eventDrill, new DefaultSecurityFailureHandler());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("Missing required authority");
        verifyNoInteractions(chain);
    }

    @Test
    void requestThatPassesAllMiddlewareReachesDownstreamFilterChain() throws Exception {
        SecurityMiddlewareRegistry registry = new DefaultSecurityMiddlewareRegistry();
        DefaultSecurityEventDrill eventDrill = new DefaultSecurityEventDrill();
        registry.register("authorization", new AuthorizationMiddleware(AuthorizationPolicies.permitAll(), eventDrill));
        SecurityPipeline pipeline = new DefaultSecurityPipeline(registry);
        SecurityMiddlewareFilter filter =
                new SecurityMiddlewareFilter(pipeline, eventDrill, new DefaultSecurityFailureHandler());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
