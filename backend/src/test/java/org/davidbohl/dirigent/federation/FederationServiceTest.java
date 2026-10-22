package org.davidbohl.dirigent.federation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.net.http.HttpTimeoutException;

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
        assertEquals("Unable to reach this instance: unexpected error.", result.get(2).error());
    }

        @Test
        void isolatesMalformedAndTimedOutRemotesWhileReturningHealthyResults() throws Exception {
                FederationProperties properties = new FederationProperties();
                FederationProperties.RemoteInstance malformed = remote("malformed");
                FederationProperties.RemoteInstance timedOut = remote("timed-out");
                FederationProperties.RemoteInstance healthy = remote("healthy");
                properties.setInstances(List.of(malformed, timedOut, healthy));

                FederationRemoteClient client = mock(FederationRemoteClient.class);
                when(client.getSystemInformation(malformed)).thenReturn(new SystemInformation("Malformed", ""));
                when(client.getDeployments(malformed)).thenThrow(new java.io.IOException("invalid JSON"));
                when(client.getSystemInformation(timedOut)).thenThrow(new HttpTimeoutException("timed out"));
                when(client.getSystemInformation(healthy)).thenReturn(new SystemInformation("Healthy", ""));
                when(client.getDeployments(healthy)).thenReturn(List.of(new DeploymentDto("api", "repo", 0,
                                DeploymentStateEntity.State.RUNNING, "healthy")));

                CachedDeploymentsConfigurationProvider configuration = mock(CachedDeploymentsConfigurationProvider.class);
                when(configuration.getCachedConfiguration()).thenReturn(new DeploynentConfiguration(List.of(), false));
                DeploymentStatePersistingService states = mock(DeploymentStatePersistingService.class);
                when(states.getDeploymentStates()).thenReturn(List.of());

                List<FederatedInstance> result = new FederationService(properties, client, configuration, states, "Local")
                                .getInstances();

                assertEquals("local", result.get(0).id());
                assertFalse(result.get(1).available());
                assertEquals("Unable to reach this instance: unexpected error.", result.get(1).error());
                assertFalse(result.get(2).available());
                assertEquals("Unable to reach this instance: the request timed out.", result.get(2).error());
                assertTrue(result.get(3).available());
                assertEquals("Healthy", result.get(3).instanceName());
                assertEquals("api", result.get(3).deployments().get(0).name());
        }

        @Test
        void describesFailureReasonsWithoutRawExceptionText() {
                assertEquals("connection failed (host unknown or refused)",
                                FederationService.describeFailure(new java.net.ConnectException("secret-host:443")));
                assertEquals("TLS or certificate validation failed",
                                FederationService.describeFailure(new javax.net.ssl.SSLHandshakeException("cert detail")));
                assertEquals("remote returned HTTP 401 (token rejected)",
                                FederationService.describeFailure(new FederationRemoteClient.RemoteStatusException(401)));
                assertEquals("remote returned HTTP 500",
                                FederationService.describeFailure(new FederationRemoteClient.RemoteStatusException(500)));
        }

        private FederationProperties.RemoteInstance remote(String id) {
                FederationProperties.RemoteInstance instance = new FederationProperties.RemoteInstance();
                instance.setId(id);
                instance.setBaseUrl("https://" + id + ".example");
                instance.setToken("read-token");
                return instance;
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