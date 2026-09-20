package org.davidbohl.dirigent.deployments.updates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class RegistryAuthPropertiesTest {

    @Test
    void findsRegistryByHostnameIgnoringCase() {
        RegistryAuthProperties.Registry registry = registry("Registry.Example.com", "user", "token");
        RegistryAuthProperties properties = properties(registry);

        assertEquals(registry, properties.findByHost("registry.example.com").orElseThrow());
    }

    @Test
    void findsRegistryByHostnameAndPort() {
        RegistryAuthProperties.Registry registry = registry("registry.example.com:5000", "user", "token");
        RegistryAuthProperties properties = properties(registry);

        assertEquals(registry, properties.findByHost("registry.example.com:5000").orElseThrow());
    }

    @Test
    void ignoresSchemeWhenMatchingConfiguredRegistry() {
        RegistryAuthProperties.Registry registry = registry("registry.example.com:5000", "user", "token");
        RegistryAuthProperties properties = properties(registry);

        assertEquals(registry, properties.findByHost("https://registry.example.com:5000").orElseThrow());
    }

    @Test
    void doesNotMatchDifferentPort() {
        RegistryAuthProperties properties = properties(
                registry("registry.example.com:5000", "user", "token"));

        assertTrue(properties.findByHost("registry.example.com:5001").isEmpty());
    }

    @Test
    void doesNotMatchDifferentHostname() {
        RegistryAuthProperties properties = properties(
                registry("registry.example.com:5000", "user", "token"));

        assertTrue(properties.findByHost("other.example.com:5000").isEmpty());
    }

    private RegistryAuthProperties properties(RegistryAuthProperties.Registry... registries) {
        RegistryAuthProperties properties = new RegistryAuthProperties();
        properties.setRegistries(List.of(registries));
        return properties;
    }

    private RegistryAuthProperties.Registry registry(String host, String username, String password) {
        RegistryAuthProperties.Registry registry = new RegistryAuthProperties.Registry();
        registry.setHost(host);
        registry.setUsername(username);
        registry.setPassword(password);
        return registry;
    }
}