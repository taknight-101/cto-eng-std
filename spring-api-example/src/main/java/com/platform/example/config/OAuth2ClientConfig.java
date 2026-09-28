package com.platform.example.config;

import com.platform.example.http.ClientCredentialsAuthMiddleware;
import com.platform.http.middleware.HttpMiddlewareRegistry;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

/**
 * Only active under the {@code docker} profile (real Keycloak). Wires the client-credentials
 * grant used for service-to-service calls to {@code downstream-api} - see
 * {@code infra/README.md} for the corresponding {@code service-client} realm configuration.
 */
@Configuration
@Profile("docker")
public class OAuth2ClientConfig {

    private static final String DOWNSTREAM_REGISTRATION_ID = "downstream-service";

    @Bean
    public OAuth2AuthorizedClientService authorizedClientService(ClientRegistrationRepository clientRegistrationRepository) {
        return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
    }

    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService authorizedClientService) {
        OAuth2AuthorizedClientProvider provider = OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build();
        AuthorizedClientServiceOAuth2AuthorizedClientManager manager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientService);
        manager.setAuthorizedClientProvider(provider);
        return manager;
    }

    @Bean
    public ApplicationRunner registerServiceAuthMiddleware(
            HttpMiddlewareRegistry registry, OAuth2AuthorizedClientManager authorizedClientManager) {
        return args -> registry.register(
                "service-auth", new ClientCredentialsAuthMiddleware(authorizedClientManager, DOWNSTREAM_REGISTRATION_ID));
    }
}
