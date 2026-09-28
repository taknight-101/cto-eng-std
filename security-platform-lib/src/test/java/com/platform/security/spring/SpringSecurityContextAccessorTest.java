package com.platform.security.spring;

import com.platform.security.api.NotAuthenticatedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpringSecurityContextAccessorTest {

    private final SpringSecurityContextAccessor accessor = new SpringSecurityContextAccessor();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requireAuthenticationThrowsWhenNoneSet() {
        assertThatThrownBy(accessor::requireAuthentication).isInstanceOf(NotAuthenticatedException.class);
    }

    @Test
    void reflectsAuthenticationInstalledOnSecurityContextHolder() {
        Authentication auth = new TestingAuthenticationToken("bob", "n/a", "ROLE_USER");
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(accessor.isAuthenticated()).isTrue();
        assertThat(accessor.hasAuthority("ROLE_USER")).isTrue();
        assertThat(accessor.hasAnyAuthority("ROLE_ADMIN", "ROLE_USER")).isTrue();
        assertThat(accessor.currentPrincipal()).isPresent();
    }

    @Test
    void runAsRestoresPreviousContextEvenOnFailure() throws Exception {
        Authentication original = new TestingAuthenticationToken("original", "n/a");
        original.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(original);

        Authentication scoped = new TestingAuthenticationToken("scoped", "n/a", "ROLE_TEMP");
        scoped.setAuthenticated(true);

        assertThatThrownBy(() -> accessor.runAs(scoped, () -> {
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(scoped);
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(original);
    }
}
