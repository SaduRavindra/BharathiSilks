package com.bharathisilks.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.beans.factory.annotation.Value;

/**
 * Registers Google only when a client id is configured, so the app still boots
 * (and tests run) without Google credentials.
 */
@Configuration
@ConditionalOnExpression("'${google.client-id:}' != ''")
public class GoogleOAuthConfig {

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(
            @Value("${google.client-id}") String clientId,
            @Value("${google.client-secret:}") String clientSecret) {
        ClientRegistration google = CommonOAuth2Provider.GOOGLE
                .getBuilder("google")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .build();
        return new InMemoryClientRegistrationRepository(google);
    }
}
