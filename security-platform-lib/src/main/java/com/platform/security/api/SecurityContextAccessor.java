package com.platform.security.api;

import org.springframework.security.core.Authentication;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * Safe, servlet-thread-scoped access to the current security state.
 *
 * <p><strong>Threading:</strong> this library is servlet-focused. Implementations back onto
 * Spring Security's own {@code SecurityContextHolder} (not a second, parallel {@code
 * ThreadLocal}), so behavior around thread pools, {@code @Async} methods and manual thread
 * hand-off matches whatever Spring Security strategy the application already uses. Reactive
 * (WebFlux) support is out of scope; a reactive equivalent would need a
 * {@code ReactiveSecurityContextAccessor} built on {@code ReactiveSecurityContextHolder}, which
 * this library deliberately does not attempt to bolt on.
 *
 * <p>{@link #runAs} is the only sanctioned way to temporarily execute code under a different
 * authentication; it always restores the previous context afterwards, even on failure.
 */
public interface SecurityContextAccessor {

    Optional<Authentication> currentAuthentication();

    Optional<SecurityPrincipal> currentPrincipal();

    Set<String> currentAuthorities();

    boolean isAuthenticated();

    /** @throws NotAuthenticatedException if nothing is currently authenticated */
    Authentication requireAuthentication();

    boolean hasAuthority(String authority);

    boolean hasAnyAuthority(String... authorities);

    <T> T runAs(Authentication authentication, Callable<T> action) throws Exception;

    void runAs(Authentication authentication, Runnable action);
}
