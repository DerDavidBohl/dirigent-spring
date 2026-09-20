package org.davidbohl.dirigent.deployments.updates;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * Per-registry credentials, e.g. via env vars:
 * DIRIGENT_REGISTRIES_0_HOST=gitea.example.com
 * DIRIGENT_REGISTRIES_0_USERNAME=myuser
 * DIRIGENT_REGISTRIES_0_PASSWORD=mytoken
 */
@Data
@ConfigurationProperties(prefix = "dirigent")
public class RegistryAuthProperties {

    private List<Registry> registries = new ArrayList<>();

    public Optional<Registry> findByHost(String host) {
        String normalizedHost = normalizeHost(host);
        return registries.stream()
                .filter(r -> r.getHost() != null && normalizeHost(r.getHost()).equals(normalizedHost))
                .findFirst();
    }

    private String normalizeHost(String host) {
        String value = host.trim();
        if (!value.contains("://"))
            value = "https://" + value;
        return URI.create(value).getAuthority().toLowerCase(Locale.ROOT);
    }

    @Data
    public static class Registry {
        private String host;
        private String username;
        private String password;
    }
}
