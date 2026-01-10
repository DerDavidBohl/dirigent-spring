package org.davidbohl.dirigent.deployments.state;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.davidbohl.dirigent.deployments.management.event.DeploymentStateEvent;
import org.davidbohl.dirigent.deployments.state.entity.DeploymentStateEntity;
import org.davidbohl.dirigent.deployments.state.event.DeploymentStateChangedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class DeploymentStatePersistingService {

    private final DeploymentStateRepository deploymentStateRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    public DeploymentStatePersistingService(DeploymentStateRepository deploymentStateRepository,
            ApplicationEventPublisher applicationEventPublisher) {
        this.deploymentStateRepository = deploymentStateRepository;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @EventListener(DeploymentStateEvent.class)
    public void handleDeploymentStateChangedEvent(DeploymentStateEvent event) {

        Optional<DeploymentStateEntity> byId = deploymentStateRepository.findById(event.getDeploymentName());

        DeploymentStateEntity deploymentState = byId.orElse(
                new DeploymentStateEntity(event.getDeploymentName(), event.getState(), event.getContext()));

        if (byId.isPresent() && byId.get().getState() == event.getState())
            return;

        deploymentState.setMessage(event.getContext());
        deploymentState.setState(event.getState());

        deploymentStateRepository.save(deploymentState);
        applicationEventPublisher.publishEvent(
                new DeploymentStateChangedEvent(this, event.getDeploymentName(), event.getState(), event.getContext()));

    }

    public List<DeploymentStateEntity> getDeploymentStates() {
        Iterable<DeploymentStateEntity> all = deploymentStateRepository.findAll();

        List<DeploymentStateEntity> result = new ArrayList<>();

        for (DeploymentStateEntity deploymentState : all) {
            result.add(deploymentState);
        }

        return result;
    }

}
