package org.davidbohl.dirigent.deployments.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class NamedDeploymentStopRequestedEvent extends ApplicationEvent {

    private final String name;

    public NamedDeploymentStopRequestedEvent(Object source, String name) {
        super(source);
        this.name = name;
    }

}
