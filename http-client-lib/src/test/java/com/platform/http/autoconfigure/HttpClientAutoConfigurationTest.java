package com.platform.http.autoconfigure;

import com.platform.http.api.HttpClient;
import com.platform.http.events.HttpEventDrill;
import com.platform.http.middleware.HttpMiddlewareRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class HttpClientAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(HttpClientAutoConfiguration.class));

    @Test
    void defaultBeansAreCreatedWhenEnabled() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(HttpClient.class);
            assertThat(context).hasSingleBean(HttpMiddlewareRegistry.class);
            assertThat(context).hasSingleBean(HttpEventDrill.class);
        });
    }

    @Test
    void noBeansAreCreatedWhenDisabled() {
        contextRunner.withPropertyValues("http-client.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(HttpClient.class));
    }
}
