package org.davidbohl.dirigent.deployments.events;

import org.springframework.context.ApplicationEvent;

public class SourceDeploymentStartRequestedEvent extends ApplicationEvent {

    private String deploymentSource;

    public SourceDeploymentStartRequestedEvent(Object source, String deploymentSource) {
        super(source);
        this.deploymentSource = deploymentSource;
    }

    public String getDeploymentSource() {
        return deploymentSource;
    }
}
