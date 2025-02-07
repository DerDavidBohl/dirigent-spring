package org.davidbohl.dirigent.deployments.state;

import org.davidbohl.dirigent.deployments.events.DeploymentStateChangedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.StreamSupport;

@Service
public class DeploymentStatePersistingService {

    final DeploymentStateRepository deploymentStateRepository;

    public DeploymentStatePersistingService(DeploymentStateRepository deploymentStateRepository) {
        this.deploymentStateRepository = deploymentStateRepository;
    }

    @EventListener(DeploymentStateChangedEvent.class)
    public void handleDeploymentStateChangedEvent(DeploymentStateChangedEvent event) {
        DeploymentState deploymentState = new DeploymentState(event.getDeploymentName(), event.getState(), event.getContext());
        deploymentStateRepository.save(deploymentState);

    }

    public List<DeploymentState> getDeploymentStates() {
        return StreamSupport.stream(deploymentStateRepository.findAll().spliterator(), false)
                .toList();
    }

}
