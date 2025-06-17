package org.davidbohl.dirigent.deployments.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class SourceDeploymentStartRequestedEvent extends ApplicationEvent {

    private final String deploymentSource;

    public SourceDeploymentStartRequestedEvent(Object source, String deploymentSource) {
        super(source);
        this.deploymentSource = deploymentSource;
    }

}
