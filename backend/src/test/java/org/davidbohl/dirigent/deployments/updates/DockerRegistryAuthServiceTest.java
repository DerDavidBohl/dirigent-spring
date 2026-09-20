package org.davidbohl.dirigent.deployments.updates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import java.util.List;

import org.davidbohl.dirigent.utility.process.ProcessResult;
import org.davidbohl.dirigent.utility.process.ProcessRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DockerRegistryAuthServiceTest {

    @Mock
    private ProcessRunner processRunner;

    @Test
    void logsInToEveryConfiguredRegistryUsingPasswordOnStandardInput() {
        RegistryAuthProperties.Registry first = registry("gitea.example.com:3000", "gitea-user", "gitea-token");
        RegistryAuthProperties.Registry second = registry("ghcr.io", "github-user", "ghcr-token");
        RegistryAuthProperties properties = properties(first, second);
        when(processRunner.executeCommandWithStdin(anyList(), anyString()))
                .thenReturn(new ProcessResult(0, "Login Succeeded", ""));

        new DockerRegistryAuthService(properties, processRunner).loginToConfiguredRegistries();

        ArgumentCaptor<List<String>> commandCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(processRunner, times(2)).executeCommandWithStdin(commandCaptor.capture(), passwordCaptor.capture());
        assertEquals(List.of("docker", "login", "gitea.example.com:3000", "-u", "gitea-user", "--password-stdin"),
                commandCaptor.getAllValues().get(0));
        assertEquals(List.of("docker", "login", "ghcr.io", "-u", "github-user", "--password-stdin"),
                commandCaptor.getAllValues().get(1));
        assertEquals(List.of("gitea-token", "ghcr-token"), passwordCaptor.getAllValues());
    }

    @Test
    void doesNothingWhenNoRegistriesAreConfigured() {
        DockerRegistryAuthService service = new DockerRegistryAuthService(properties(), processRunner);

        service.loginToConfiguredRegistries();

        verifyNoMoreInteractions(processRunner);
    }

    @Test
    void throwsWhenDockerLoginFails() {
        RegistryAuthProperties.Registry registry = registry("gitea.example.com", "user", "token");
        RegistryAuthProperties properties = properties(registry);
        when(processRunner.executeCommandWithStdin(anyList(), anyString()))
                .thenReturn(new ProcessResult(1, "", "unauthorized"));

        RegistryAuthenticationException exception = assertThrows(
                RegistryAuthenticationException.class,
                () -> new DockerRegistryAuthService(properties, processRunner).loginToConfiguredRegistries());

        assertEquals("Failed to login to registry gitea.example.com: unauthorized", exception.getMessage());
    }

    @Test
    void stopsAfterFirstFailedLogin() {
        RegistryAuthProperties properties = properties(
                registry("first.example.com", "user", "bad-token"),
                registry("second.example.com", "user", "token"));
        when(processRunner.executeCommandWithStdin(anyList(), anyString()))
                .thenReturn(new ProcessResult(1, "", "unauthorized"));

        assertThrows(RegistryAuthenticationException.class,
                () -> new DockerRegistryAuthService(properties, processRunner).loginToConfiguredRegistries());

        verify(processRunner).executeCommandWithStdin(anyList(), eq("bad-token"));
        verifyNoMoreInteractions(processRunner);
    }

    @Test
    void throwsForMissingHost() {
        assertThrowsForIncompleteRegistry(null, "user", "token", "null");
    }

    @Test
    void throwsForMissingUsername() {
        assertThrowsForIncompleteRegistry("registry.example.com", null, "token", "registry.example.com");
    }

    @Test
    void throwsForMissingPassword() {
        assertThrowsForIncompleteRegistry("registry.example.com", "user", null, "registry.example.com");
    }

    private void assertThrowsForIncompleteRegistry(String host, String username, String password, String messageHost) {
        RegistryAuthProperties properties = properties(registry(host, username, password));

        RegistryAuthenticationException exception = assertThrows(
                RegistryAuthenticationException.class,
                () -> new DockerRegistryAuthService(properties, processRunner).loginToConfiguredRegistries());

        assertEquals("Incomplete credentials for registry " + messageHost, exception.getMessage());
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