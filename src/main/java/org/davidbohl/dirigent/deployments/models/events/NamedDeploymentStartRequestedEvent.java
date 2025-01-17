package org.davidbohl.dirigent.deployments.models.events;

import org.springframework.context.ApplicationEvent;

public class NamedDeploymentStartRequestedEvent extends ApplicationEvent {

    private String name;

    public NamedDeploymentStartRequestedEvent(Object source, String name) {
        super(source);
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
