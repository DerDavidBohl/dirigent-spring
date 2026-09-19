package org.davidbohl.dirigent.deployments.updates;

import java.util.ArrayList;
import java.util.List;
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
        return registries.stream()
                .filter(r -> r.getHost() != null && r.getHost().equalsIgnoreCase(host))
                .findFirst();
    }

    @Data
    public static class Registry {
        private String host;
        private String username;
        private String password;
    }
}
