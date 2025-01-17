package org.davidbohl.dirigent.deployments.models.events;

import org.springframework.context.ApplicationEvent;

public class DeploymentStartFailedEvent extends ApplicationEvent {


    private final String deploymentName;
    private final String message;

    public DeploymentStartFailedEvent(Object source, String deploymentName, String string) {
        super(source);
        this.deploymentName = deploymentName;
        this.message = string;
    }

    public String getMessage() {
        return message;
    }

    public String getDeploymentName() {
        return deploymentName;
    }
}
