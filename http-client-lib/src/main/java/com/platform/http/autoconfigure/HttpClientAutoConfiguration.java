package com.platform.http.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.platform.http.events.DefaultHttpEventDrill;
import com.platform.http.events.HttpEventDrill;
import com.platform.http.events.LoggingHttpEventConsumer;
import com.platform.http.internal.DefaultHttpClient;
import com.platform.http.internal.JacksonBodyDeserializer;
import com.platform.http.internal.JacksonBodySerializer;
import com.platform.http.middleware.DefaultHttpMiddlewareRegistry;
import com.platform.http.middleware.HttpMiddlewareRegistry;
import com.platform.http.spi.BodyDeserializer;
import com.platform.http.spi.BodySerializer;
import com.platform.http.spi.RetryPolicy;
import com.platform.http.spring.HttpEngine;
import com.platform.http.spring.WebClientHttpEngine;
import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Set;

/** Wires the HTTP client platform beans behind the internal {@link HttpEngine} adapter boundary. */
@AutoConfiguration
@EnableConfigurationProperties(HttpClientProperties.class)
@ConditionalOnProperty(prefix = "http-client", name = "enabled", havingValue = "true", matchIfMissing = true)
public class HttpClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper httpClientObjectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Bean
    @ConditionalOnMissingBean
    public BodySerializer bodySerializer(ObjectMapper httpClientObjectMapper) {
        return new JacksonBodySerializer(httpClientObjectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public BodyDeserializer bodyDeserializer(ObjectMapper httpClientObjectMapper) {
        return new JacksonBodyDeserializer(httpClientObjectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public HttpMiddlewareRegistry httpMiddlewareRegistry() {
        return new DefaultHttpMiddlewareRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public HttpEventDrill httpEventDrill() {
        return new DefaultHttpEventDrill();
    }

    @Bean
    @ConditionalOnMissingBean
    public RetryPolicy retryPolicy(HttpClientProperties properties) {
        HttpClientProperties.Retry retry = properties.getRetry();
        return new RetryPolicy(retry.getMaxAttempts(), retry.getInitialBackoff(), retry.getBackoffMultiplier(),
                retry.getMaxBackoff(), Set.copyOf(retry.getRetryableStatusCodes()), retry.isIdempotentOnly());
    }

    @Bean
    @ConditionalOnMissingBean
    public WebClient httpClientWebClient(HttpClientProperties properties, ObjectProvider<WebClient.Builder> builders) {
        reactor.netty.http.client.HttpClient nettyClient = reactor.netty.http.client.HttpClient.create()
                .responseTimeout(properties.getDefaultTimeout())
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) properties.getConnectTimeout().toMillis());
        WebClient.Builder builder = builders.getIfAvailable(WebClient::builder);
        return builder.clientConnector(new ReactorClientHttpConnector(nettyClient)).build();
    }

    @Bean
    @ConditionalOnMissingBean
    public HttpEngine httpEngine(WebClient httpClientWebClient, BodySerializer bodySerializer) {
        return new WebClientHttpEngine(httpClientWebClient, bodySerializer);
    }

    @Bean
    @ConditionalOnMissingBean
    public com.platform.http.api.HttpClient httpClient(
            HttpEngine httpEngine,
            HttpMiddlewareRegistry httpMiddlewareRegistry,
            HttpEventDrill httpEventDrill,
            BodyDeserializer bodyDeserializer,
            RetryPolicy retryPolicy) {
        return new DefaultHttpClient(httpEngine, httpMiddlewareRegistry, httpEventDrill, bodyDeserializer, retryPolicy);
    }

    @Bean
    public HttpEventDrillInitializer httpEventDrillInitializer(HttpEventDrill httpEventDrill, HttpClientProperties properties) {
        return new HttpEventDrillInitializer(httpEventDrill, properties);
    }

    /** Subscribes the default logging consumer once, unless the app disabled it via properties. */
    static final class HttpEventDrillInitializer implements InitializingBean {
        private final HttpEventDrill eventDrill;
        private final HttpClientProperties properties;

        HttpEventDrillInitializer(HttpEventDrill eventDrill, HttpClientProperties properties) {
            this.eventDrill = eventDrill;
            this.properties = properties;
        }

        @Override
        public void afterPropertiesSet() {
            if (properties.getEvents().isLoggingConsumerEnabled()) {
                eventDrill.subscribe(new LoggingHttpEventConsumer());
            }
        }
    }
}
