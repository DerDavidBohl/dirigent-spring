package org.davidbohl.dirigent.deployments.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class NamedDeploymentStartRequestedEvent extends ApplicationEvent {

    private final String name;
    private final boolean forced;

    public NamedDeploymentStartRequestedEvent(Object source, String name, boolean forced) {
        super(source);
        this.name = name;
        this.forced = forced;
    }

}
