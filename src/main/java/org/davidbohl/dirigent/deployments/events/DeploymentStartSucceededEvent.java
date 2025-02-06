package org.davidbohl.dirigent.deployments.events;

import org.springframework.context.ApplicationEvent;

public class DeploymentStartSucceededEvent extends ApplicationEvent {
    private final String deploymentName;

    public DeploymentStartSucceededEvent(Object source, String deploymentName) {
        super(source);
        this.deploymentName = deploymentName;
    }

    public String getDeploymentName() {
        return deploymentName;
    }
}
