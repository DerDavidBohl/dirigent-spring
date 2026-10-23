package org.davidbohl.dirigent.deployments.management;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.davidbohl.dirigent.deployments.config.DeploymentsConfigurationProvider;
import org.davidbohl.dirigent.deployments.config.model.Deployment;
import org.davidbohl.dirigent.deployments.config.model.DeploynentConfiguration;
import org.davidbohl.dirigent.deployments.management.event.DeploymentStateEvent;
import org.davidbohl.dirigent.deployments.state.DeploymentStatePersistingService;
import org.davidbohl.dirigent.deployments.state.entity.DeploymentStateEntity;
import org.davidbohl.dirigent.deployments.updates.DockerRegistryAuthService;
import org.davidbohl.dirigent.sercrets.SecretService;
import org.davidbohl.dirigent.utility.git.GitService;
import org.davidbohl.dirigent.utility.process.ProcessResult;
import org.davidbohl.dirigent.utility.process.ProcessRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

// REQ-007: removed deployments are reconciled by stopping them and deleting their checkout.
class DeploymentsServiceTest {

    @TempDir
    Path deploymentsDir;

    private DeploymentsConfigurationProvider provider;
    private DeploymentStatePersistingService states;
    private ProcessRunner processRunner;
    private ApplicationEventPublisher publisher;
    private DeploymentsService service;

    @BeforeEach
    void setUp() throws Exception {
        provider = mock(DeploymentsConfigurationProvider.class);
        states = mock(DeploymentStatePersistingService.class);
        processRunner = mock(ProcessRunner.class);
        publisher = mock(ApplicationEventPublisher.class);
        when(provider.getConfiguration()).thenReturn(new DeploynentConfiguration(List.of(), false));
        when(processRunner.executeCommand(any(), any(File.class))).thenReturn(new ProcessResult(0, "", ""));

        service = new DeploymentsService(mock(GitService.class), provider, publisher, states,
                mock(SecretService.class), processRunner, mock(DockerRegistryAuthService.class));
        ReflectionTestUtils.setField(service, "composeCommand", "docker compose");
        ReflectionTestUtils.setField(service, "deploymentsDirName", deploymentsDir.toString());
    }

    @Test
    void removedDeploymentIsStoppedBeforeItsCheckoutIsDeletedAndReportedRemoved() throws Exception {
        Path checkout = createCheckout("old");
        List<Boolean> checkoutExistedOnDown = new ArrayList<>();
        when(processRunner.executeCommand(any(), any(File.class))).thenAnswer(invocation -> {
            checkoutExistedOnDown.add(Files.exists(checkout.resolve("docker-compose.yml")));
            return new ProcessResult(0, "", "");
        });

        service.onRecreateAllDeploymentStatesEvent();

        verify(processRunner).executeCommand(List.of("docker", "compose", "down"), checkout.toFile());
        assertEquals(List.of(true), checkoutExistedOnDown);
        assertTrue(Files.notExists(checkout));
        assertTrue(publishedStates("old").contains(DeploymentStateEntity.State.REMOVED));
    }

    @Test
    void checkoutIsDeletedAndRemovedIsPublishedEvenWhenComposeDownExitsNonZero() throws Exception {
        Path checkout = createCheckout("old");
        when(processRunner.executeCommand(any(), any(File.class))).thenReturn(new ProcessResult(1, "", "boom"));

        service.onRecreateAllDeploymentStatesEvent();

        assertTrue(Files.notExists(checkout));
        assertTrue(publishedStates("old").contains(DeploymentStateEntity.State.REMOVED));
    }

    @Test
    void thrownProcessErrorAbortsRemovalAndKeepsCheckout() throws Exception {
        Path checkout = createCheckout("old");
        when(processRunner.executeCommand(any(), any(File.class))).thenThrow(new IllegalStateException("no docker"));

        assertThrows(RuntimeException.class, () -> service.onRecreateAllDeploymentStatesEvent());

        assertTrue(Files.exists(checkout));
        assertTrue(!publishedStates("old").contains(DeploymentStateEntity.State.REMOVED));
    }

    @Test
    void allDeploymentsStartAlsoRemovesUnconfiguredCheckouts() throws Exception {
        Path checkout = createCheckout("old");

        service.onAllDeploymentsStartRequested(
                new org.davidbohl.dirigent.deployments.management.event.AllDeploymentsStartRequestedEvent(this,
                        false));

        assertTrue(Files.notExists(checkout));
        assertTrue(publishedStates("old").contains(DeploymentStateEntity.State.REMOVED));
    }

    @Test
    void configuredCheckoutIsNotRemoved() throws Exception {
        Path checkout = createCheckout("kept");
        when(provider.getConfiguration())
                .thenReturn(new DeploynentConfiguration(List.of(new Deployment("kept", "src", 0, null)), false));
        // Only the removal path is under test; the deploy of "kept" is handled by mocked collaborators.
        when(processRunner.executeCommand(any(), any(File.class), any())).thenReturn(new ProcessResult(0, "", ""));
        ReflectionTestUtils.setField(service, "dirigentHostDeploymentsDir", "/tmp");

        service.onRecreateAllDeploymentStatesEvent();

        assertTrue(Files.exists(checkout));
        assertTrue(!publishedStates("kept").contains(DeploymentStateEntity.State.REMOVED));
    }

    @Test
    void recreateAllIgnoresStoppedStateOfUnconfiguredDeploymentWithoutDirectory() throws Exception {
        when(states.getDeploymentStates()).thenReturn(List.of(
                new DeploymentStateEntity("gone", DeploymentStateEntity.State.STOPPED, "")));

        service.onRecreateAllDeploymentStatesEvent();

        verify(processRunner, never()).executeCommand(any(), eq(deploymentsDir.resolve("gone").toFile()));
    }

    private Path createCheckout(String name) throws IOException {
        Path checkout = Files.createDirectories(deploymentsDir.resolve(name));
        Files.writeString(checkout.resolve("docker-compose.yml"), "services: {}\n");
        return checkout;
    }

    private List<DeploymentStateEntity.State> publishedStates(String name) {
        ArgumentCaptor<ApplicationEvent> captor = ArgumentCaptor.forClass(ApplicationEvent.class);
        verify(publisher, atLeastOnce()).publishEvent(captor.capture());
        return captor.getAllValues().stream()
                .filter(DeploymentStateEvent.class::isInstance)
                .map(DeploymentStateEvent.class::cast)
                .filter(e -> name.equals(e.getDeploymentName()))
                .map(DeploymentStateEvent::getState)
                .toList();
    }
}
