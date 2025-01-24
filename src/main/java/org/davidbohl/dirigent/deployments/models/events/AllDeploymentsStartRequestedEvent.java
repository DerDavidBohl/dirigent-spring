package org.davidbohl.dirigent.deployments.models.events;

import org.springframework.context.ApplicationEvent;

public class AllDeploymentsStartRequestedEvent extends ApplicationEvent {

    private final boolean forced;

    public AllDeploymentsStartRequestedEvent(Object source, boolean forced) {
        super(source);
        this.forced = forced;
    }

    public boolean isForced() {
        return forced;
    }
}
