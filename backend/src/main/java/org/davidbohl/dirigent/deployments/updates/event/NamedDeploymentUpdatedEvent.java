package org.davidbohl.dirigent.deployments.updates.event;

import org.springframework.context.ApplicationEvent;

import lombok.Getter;

@Getter
public class NamedDeploymentUpdatedEvent extends ApplicationEvent {

    private String deploymentName;

    public NamedDeploymentUpdatedEvent(Object source, String deploymentName) {
        super(source);
        this.deploymentName = deploymentName;
    }

}
