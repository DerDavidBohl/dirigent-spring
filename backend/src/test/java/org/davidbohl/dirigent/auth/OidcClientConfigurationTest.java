package org.davidbohl.dirigent.auth;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class OidcClientConfigurationTest {

    @Test
    void rejectsHttpIssuerBeforeProviderDiscovery() {
        OidcClientProperties properties = new OidcClientProperties();
        properties.setClientId("client-id");
        properties.setClientSecret("client-secret");
        properties.setIssuerUri("http://identity.example.test");

        assertThrows(IllegalStateException.class,
                () -> new OidcClientConfiguration().clientRegistrationRepository(properties));
    }
}