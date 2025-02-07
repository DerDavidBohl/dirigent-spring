package org.davidbohl.dirigent.deployments.state;

import org.davidbohl.dirigent.deployments.events.DeploymentStateChangedEvent;
import org.davidbohl.dirigent.deployments.events.DeploymentStateEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Service
public class DeploymentStatePersistingService {

    private final DeploymentStateRepository deploymentStateRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    public DeploymentStatePersistingService(DeploymentStateRepository deploymentStateRepository, ApplicationEventPublisher applicationEventPublisher) {
        this.deploymentStateRepository = deploymentStateRepository;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @EventListener(DeploymentStateEvent.class)
    public void handleDeploymentStateChangedEvent(DeploymentStateEvent event) {

        Optional<DeploymentState> byId = deploymentStateRepository.findById(event.getDeploymentName());

        if (byId.isPresent() && byId.get().getState() == event.getState())
            return;

        DeploymentState deploymentState = new DeploymentState(event.getDeploymentName(), event.getState(), event.getContext());
        deploymentStateRepository.save(deploymentState);
        applicationEventPublisher.publishEvent(new DeploymentStateChangedEvent(this, event.getDeploymentName(), event.getState(), event.getContext()));

    }

    public List<DeploymentState> getDeploymentStates() {
        return StreamSupport.stream(deploymentStateRepository.findAll().spliterator(), false)
                .toList();
    }

}
