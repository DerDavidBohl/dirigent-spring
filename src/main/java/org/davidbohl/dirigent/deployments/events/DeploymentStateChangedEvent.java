package org.davidbohl.dirigent.deployments.events;

import lombok.Getter;
import org.davidbohl.dirigent.deployments.state.DeploymentState;
import org.springframework.context.ApplicationEvent;

@Getter
public class DeploymentStateChangedEvent extends ApplicationEvent {

    final String deploymentName;
    final DeploymentState.State state;
    final String context;

    public DeploymentStateChangedEvent(Object source, String deploymentName, DeploymentState.State state, String context) {
        super(source);
        this.deploymentName = deploymentName;
        this.state = state;
        this.context = context;
    }

}
