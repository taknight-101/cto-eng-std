package com.platform.security.autoconfigure;

import com.platform.security.api.AuthorizationPolicy;
import com.platform.security.api.SecurityContextAccessor;
import com.platform.security.api.SecurityEventDrill;
import com.platform.security.api.SecurityMiddlewareRegistry;
import com.platform.security.api.SecurityPipeline;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityPlatformAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SecurityPlatformAutoConfiguration.class));

    @Test
    void defaultBeansAreCreatedWhenEnabled() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(SecurityMiddlewareRegistry.class);
            assertThat(context).hasSingleBean(SecurityPipeline.class);
            assertThat(context).hasSingleBean(SecurityEventDrill.class);
            assertThat(context).hasSingleBean(SecurityContextAccessor.class);
            assertThat(context).hasSingleBean(AuthorizationPolicy.class);
            assertThat(context.getBean(SecurityMiddlewareRegistry.class).registrations()).hasSize(2);
        });
    }

    @Test
    void noBeansAreCreatedWhenDisabled() {
        contextRunner.withPropertyValues("security-platform.enabled=false").run(context -> {
            assertThat(context).doesNotHaveBean(SecurityPipeline.class);
            assertThat(context).doesNotHaveBean(SecurityMiddlewareRegistry.class);
        });
    }

    @Test
    void jwtAuthenticatorRequiresExplicitEnable() {
        contextRunner.run(context ->
                assertThat(context).doesNotHaveBean(com.platform.security.api.JwtAuthenticator.class));

        contextRunner.withPropertyValues(
                "security-platform.jwt.enabled=true",
                "security-platform.jwt.hmac-secret=test-secret-key-at-least-32-bytes-long!!"
        ).run(context -> assertThat(context).hasSingleBean(com.platform.security.api.JwtAuthenticator.class));
    }
}
