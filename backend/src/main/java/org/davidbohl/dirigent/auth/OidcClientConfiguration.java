package org.davidbohl.dirigent.auth;

import java.net.URI;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

@Configuration
@ConditionalOnProperty(prefix = "dirigent.oidc", name = "enabled", havingValue = "true")
public class OidcClientConfiguration {

    @Bean
    ClientRegistrationRepository clientRegistrationRepository(OidcClientProperties properties) {
        if (isBlank(properties.getClientId()) || isBlank(properties.getClientSecret())
                || isBlank(properties.getIssuerUri())) {
            throw new IllegalStateException(
                    "OIDC requires DIRIGENT_OIDC_CLIENT_ID, DIRIGENT_OIDC_CLIENT_SECRET, and DIRIGENT_OIDC_ISSUER_URI");
        }
        URI issuerUri = URI.create(properties.getIssuerUri());
        if (!issuerUri.isAbsolute() || !"https".equalsIgnoreCase(issuerUri.getScheme())
                || issuerUri.getHost() == null) {
            throw new IllegalStateException("DIRIGENT_OIDC_ISSUER_URI must be an absolute HTTPS URL");
        }

        ClientRegistration registration = ClientRegistrations.fromIssuerLocation(properties.getIssuerUri())
                .registrationId("dirigent")
                .clientId(properties.getClientId())
                .clientSecret(properties.getClientSecret())
                .scope(properties.getScope())
                .clientName("Dirigent")
                .build();

        return new InMemoryClientRegistrationRepository(registration);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}