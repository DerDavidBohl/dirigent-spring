package org.davidbohl.dirigent.deployments.state.event;

import lombok.Getter;

import org.davidbohl.dirigent.deployments.state.entity.DeploymentStateEntity;
import org.springframework.context.ApplicationEvent;

@Getter
public class DeploymentStateChangedEvent extends ApplicationEvent {

    final String deploymentName;
    final DeploymentStateEntity.State state;
    final String context;

    public DeploymentStateChangedEvent(Object source, String deploymentName, DeploymentStateEntity.State state, String context) {
        super(source);
        this.deploymentName = deploymentName;
        this.state = state;
        this.context = context;
    }

}
