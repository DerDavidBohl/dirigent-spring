package org.davidbohl.dirigent.deployments.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class AllDeploymentsStartRequestedEvent extends ApplicationEvent {

    private final boolean forceRecreate;

    public AllDeploymentsStartRequestedEvent(Object source, boolean forceRecreate) {
        super(source);
        this.forceRecreate = forceRecreate;
    }
}
