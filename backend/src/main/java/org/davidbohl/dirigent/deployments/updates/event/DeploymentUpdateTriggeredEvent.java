package org.davidbohl.dirigent.deployments.updates.event;

import org.davidbohl.dirigent.deployments.updates.dto.DeploymentUpdateDto;
import org.springframework.context.ApplicationEvent;

public class DeploymentUpdateTriggeredEvent extends ApplicationEvent {
    private final DeploymentUpdateDto deploymentUpdate;

    public DeploymentUpdateTriggeredEvent(Object source, DeploymentUpdateDto deploymentUpdate) {
        super(source);
        this.deploymentUpdate = deploymentUpdate;
    }

    public DeploymentUpdateDto getDeploymentUpdate() {
        return deploymentUpdate;
    }
}
