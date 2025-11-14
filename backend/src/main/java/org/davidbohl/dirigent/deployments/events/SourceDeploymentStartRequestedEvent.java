package org.davidbohl.dirigent.deployments.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class SourceDeploymentStartRequestedEvent extends ApplicationEvent {

    private final String deploymentSource;
    private final String ref;

    public SourceDeploymentStartRequestedEvent(Object source, String deploymentSource, String ref) {
        super(source);
        this.deploymentSource = deploymentSource;
        this.ref = ref;
    }

}
