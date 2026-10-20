package org.davidbohl.dirigent.federation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.davidbohl.dirigent.deployments.config.CachedDeploymentsConfigurationProvider;
import org.davidbohl.dirigent.deployments.config.model.Deployment;
import org.davidbohl.dirigent.deployments.config.model.DeploynentConfiguration;
import org.davidbohl.dirigent.deployments.management.model.DeploymentDto;
import org.davidbohl.dirigent.deployments.state.DeploymentStatePersistingService;
import org.davidbohl.dirigent.deployments.state.entity.DeploymentStateEntity;
import org.davidbohl.dirigent.system.SystemInformation;
import org.junit.jupiter.api.Test;

class FederationServiceTest {

    @Test
    void aggregatesLocalAndRemoteInstancesAndIsolatesRemoteFailure() throws Exception {
        FederationProperties properties = new FederationProperties();
        FederationProperties.RemoteInstance remote = new FederationProperties.RemoteInstance();
        remote.setId("office");
        remote.setBaseUrl("https://office.example");
        remote.setToken("read-token");
        FederationProperties.RemoteInstance unreachable = new FederationProperties.RemoteInstance();
        unreachable.setId("garage");
        unreachable.setBaseUrl("https://garage.example");
        unreachable.setToken("read-token");
        properties.setInstances(List.of(remote, unreachable));

        FederationRemoteClient client = mock(FederationRemoteClient.class);
        when(client.getSystemInformation(remote)).thenReturn(new SystemInformation("Office", ""));
        when(client.getDeployments(remote)).thenReturn(List.of(new DeploymentDto("api", "repo", 0,
                DeploymentStateEntity.State.RUNNING, "healthy")));
        when(client.getSystemInformation(unreachable)).thenThrow(new java.io.IOException("private detail"));

        CachedDeploymentsConfigurationProvider configuration = mock(CachedDeploymentsConfigurationProvider.class);
        when(configuration.getCachedConfiguration()).thenReturn(new DeploynentConfiguration(
                List.of(new Deployment("web", "repo", 0, null)), false));
        DeploymentStatePersistingService states = mock(DeploymentStatePersistingService.class);
        when(states.getDeploymentStates()).thenReturn(List.of());

        List<FederatedInstance> result = new FederationService(properties, client, configuration, states, "Local")
                .getInstances();

        assertEquals(3, result.size());
        assertEquals("local", result.get(0).id());
        assertEquals("UNKNOWN", result.get(0).deployments().get(0).state());
        assertTrue(result.get(1).available());
        assertEquals("Office", result.get(1).instanceName());
        assertEquals("api", result.get(1).deployments().get(0).name());
        assertFalse(result.get(2).available());
        assertEquals("Unable to reach this instance.", result.get(2).error());
    }

    @Test
    void rejectsDuplicateOrInvalidRemoteIdsForWriteRouting() {
        FederationProperties properties = new FederationProperties();
        FederationProperties.RemoteInstance first = new FederationProperties.RemoteInstance();
        first.setId("same");
        FederationProperties.RemoteInstance duplicate = new FederationProperties.RemoteInstance();
        duplicate.setId("same");
        properties.setInstances(List.of(first, duplicate));
        FederationService service = new FederationService(properties, mock(FederationRemoteClient.class),
                mock(CachedDeploymentsConfigurationProvider.class), mock(DeploymentStatePersistingService.class), "Local");

        org.junit.jupiter.api.Assertions.assertThrows(FederationService.UnknownFederatedInstanceException.class,
                () -> service.getRemote("same"));
        org.junit.jupiter.api.Assertions.assertThrows(FederationService.UnknownFederatedInstanceException.class,
                () -> service.getRemote("../other"));
    }
}