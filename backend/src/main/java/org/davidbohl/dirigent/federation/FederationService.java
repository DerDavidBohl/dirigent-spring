package org.davidbohl.dirigent.federation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.davidbohl.dirigent.deployments.config.CachedDeploymentsConfigurationProvider;
import org.davidbohl.dirigent.deployments.management.model.DeploymentDto;
import org.davidbohl.dirigent.deployments.state.DeploymentStatePersistingService;
import org.davidbohl.dirigent.deployments.state.entity.DeploymentStateEntity;
import org.davidbohl.dirigent.system.SystemInformation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

@Service
public class FederationService {

    private final FederationProperties properties;
    private final FederationRemoteClient remoteClient;
    private final CachedDeploymentsConfigurationProvider configurationProvider;
    private final DeploymentStatePersistingService stateService;
    private final String localInstanceName;

    public FederationService(FederationProperties properties, FederationRemoteClient remoteClient,
            CachedDeploymentsConfigurationProvider configurationProvider,
            DeploymentStatePersistingService stateService,
            @Value("${dirigent.instanceName:Unknown Instance}") String localInstanceName) {
        this.properties = properties;
        this.remoteClient = remoteClient;
        this.configurationProvider = configurationProvider;
        this.stateService = stateService;
        this.localInstanceName = localInstanceName;
    }

    public List<FederatedInstance> getInstances() {
        List<FederatedInstance> instances = new ArrayList<>();
        instances.add(getLocalInstance());
        Set<String> usedIds = new HashSet<>();
        usedIds.add("local");
        int index = 0;
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<FederatedInstance>> requests = new ArrayList<>();
            for (FederationProperties.RemoteInstance remote : properties.getInstances()) {
                String id = remote.getId();
                if (!isValidId(id) || !usedIds.add(id)) {
                    requests.add(CompletableFuture.completedFuture(unavailable("invalid-instance-" + index)));
                } else {
                    requests.add(CompletableFuture.supplyAsync(() -> getRemoteInstance(id, remote, executor), executor));
                }
                index++;
            }
            for (CompletableFuture<FederatedInstance> request : requests) {
                instances.add(request.join());
            }
        }
        return List.copyOf(instances);
    }

    public FederationProperties.RemoteInstance getRemote(String id) {
        if (id == null || "local".equals(id)) {
            throw new UnknownFederatedInstanceException();
        }
        List<FederationProperties.RemoteInstance> matches = properties.getInstances().stream()
                .filter(instance -> id.equals(instance.getId()))
                .toList();
        if (matches.size() != 1 || !isValidId(id)) {
            throw new UnknownFederatedInstanceException();
        }
        return matches.get(0);
    }

    public FederationRemoteClient.RemoteResponse forward(String id, HttpMethod method, String path,
                                                          String query, Object body) throws Exception {
        return remoteClient.forward(getRemote(id), method, path, query, body);
    }

    private FederatedInstance getLocalInstance() {
        Map<String, DeploymentStateEntity> states = new HashMap<>();
        stateService.getDeploymentStates().forEach(state -> states.put(state.getName(), state));
        List<FederatedDeployment> deployments = configurationProvider.getCachedConfiguration().deployments().stream()
                .map(deployment -> {
                    DeploymentStateEntity state = states.get(deployment.name());
                    return new FederatedDeployment(deployment.name(),
                            state == null ? DeploymentStateEntity.State.UNKNOWN.name() : state.getState().name(),
                            state == null ? "" : state.getMessage());
                })
                .toList();
        return new FederatedInstance("local", localInstanceName, true, deployments, "");
    }

    private FederatedInstance getRemoteInstance(String id, FederationProperties.RemoteInstance remote,
                                                 Executor executor) {
        if (!isValidId(id)) {
            return unavailable(id);
        }
        try {
            CompletableFuture<SystemInformation> information = callRemoteAsync(
                    executor, () -> remoteClient.getSystemInformation(remote));
            CompletableFuture<List<DeploymentDto>> remoteDeployments = callRemoteAsync(
                    executor, () -> remoteClient.getDeployments(remote));
            List<FederatedDeployment> deployments = remoteDeployments.join().stream()
                    .map(this::toFederatedDeployment)
                    .toList();
            return new FederatedInstance(id, information.join().instanceName(), true, deployments, "");
        } catch (CompletionException exception) {
            return unavailable(id);
        } catch (Exception exception) {
            return unavailable(id);
        }
    }

    private <T> CompletableFuture<T> callRemoteAsync(Executor executor, RemoteCall<T> call) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return call.get();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new CompletionException(exception);
            } catch (Exception exception) {
                throw new CompletionException(exception);
            }
        }, executor);
    }

    private FederatedDeployment toFederatedDeployment(DeploymentDto deployment) {
        return new FederatedDeployment(deployment.name(), deployment.state().name(), deployment.message());
    }

    private FederatedInstance unavailable(String id) {
        return new FederatedInstance(id, id, false, List.of(), "Unable to reach this instance.");
    }

    private boolean isValidId(String id) {
        return id != null && id.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,63}") && !"local".equals(id);
    }

    @FunctionalInterface
    private interface RemoteCall<T> {
        T get() throws Exception;
    }

    public static class UnknownFederatedInstanceException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}