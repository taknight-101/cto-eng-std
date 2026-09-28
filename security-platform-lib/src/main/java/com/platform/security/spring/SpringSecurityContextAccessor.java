package com.platform.security.spring;

import com.platform.security.api.NotAuthenticatedException;
import com.platform.security.api.SecurityContextAccessor;
import com.platform.security.api.SecurityPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * {@link SecurityContextAccessor} backed directly by Spring Security's own {@code
 * SecurityContextHolder} - deliberately not a second, parallel {@code ThreadLocal} - so this
 * accessor always agrees with whatever the rest of Spring Security sees.
 */
public final class SpringSecurityContextAccessor implements SecurityContextAccessor {

    @Override
    public Optional<Authentication> currentAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (authentication != null && authentication.isAuthenticated())
                ? Optional.of(authentication) : Optional.empty();
    }

    @Override
    public Optional<SecurityPrincipal> currentPrincipal() {
        return currentAuthentication().map(auth -> SecurityPrincipal.of(String.valueOf(auth.getPrincipal())));
    }

    @Override
    public Set<String> currentAuthorities() {
        return currentAuthentication()
                .map(auth -> auth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toUnmodifiableSet()))
                .orElse(Set.of());
    }

    @Override
    public boolean isAuthenticated() {
        return currentAuthentication().isPresent();
    }

    @Override
    public Authentication requireAuthentication() {
        return currentAuthentication().orElseThrow(NotAuthenticatedException::new);
    }

    @Override
    public boolean hasAuthority(String authority) {
        return currentAuthorities().contains(authority);
    }

    @Override
    public boolean hasAnyAuthority(String... authorities) {
        Set<String> current = currentAuthorities();
        return Arrays.stream(authorities).anyMatch(current::contains);
    }

    @Override
    public <T> T runAs(Authentication authentication, Callable<T> action) throws Exception {
        SecurityContext previous = SecurityContextHolder.getContext();
        SecurityContext scoped = SecurityContextHolder.createEmptyContext();
        scoped.setAuthentication(authentication);
        SecurityContextHolder.setContext(scoped);
        try {
            return action.call();
        } finally {
            SecurityContextHolder.setContext(previous);
        }
    }

    @Override
    public void runAs(Authentication authentication, Runnable action) {
        try {
            runAs(authentication, () -> {
                action.run();
                return null;
            });
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new IllegalStateException("runAs action failed", e);
        }
    }
}
