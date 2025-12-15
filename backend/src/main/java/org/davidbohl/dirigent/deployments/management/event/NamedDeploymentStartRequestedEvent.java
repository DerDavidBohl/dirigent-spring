package org.davidbohl.dirigent.deployments.management.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class NamedDeploymentStartRequestedEvent extends ApplicationEvent {

    private final String name;
    private final boolean forceRecreate;

    public NamedDeploymentStartRequestedEvent(Object source, String name, boolean forceRecreate) {
        super(source);
        this.name = name;
        this.forceRecreate = forceRecreate;
    }

}
