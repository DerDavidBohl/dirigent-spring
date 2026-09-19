package org.davidbohl.dirigent.deployments.updates;

import java.util.List;

import org.davidbohl.dirigent.utility.process.ProcessResult;
import org.davidbohl.dirigent.utility.process.ProcessRunner;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Runs "docker login" for all configured registries so that "docker compose up"
 * pulls of private images succeed, in addition to the update checks.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DockerRegistryAuthService {

    private final RegistryAuthProperties registryAuthProperties;
    private final ProcessRunner processRunner;

    public void loginToConfiguredRegistries() {
        for (RegistryAuthProperties.Registry registry : registryAuthProperties.getRegistries()) {
            login(registry);
        }
    }

    private void login(RegistryAuthProperties.Registry registry) {
        if (registry.getHost() == null || registry.getUsername() == null || registry.getPassword() == null) {
            log.warn("Skipping incomplete registry credentials for host {}", registry.getHost());
            return;
        }

        List<String> commandArgs = List.of("docker", "login", registry.getHost(), "-u", registry.getUsername(), "--password-stdin");
        ProcessResult result = processRunner.executeCommandWithStdin(commandArgs, registry.getPassword());

        if (result.exitCode() != 0)
            log.warn("Failed to login to registry {}: {}", registry.getHost(), result.stderr());
        else
            log.info("Logged in to registry {}", registry.getHost());
    }
}
