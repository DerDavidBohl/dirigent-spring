package org.davidbohl.dirigent.deployments.models.events;

import org.springframework.context.ApplicationEvent;

public class AllDeploymentsStartRequestedEvent extends ApplicationEvent {
    public AllDeploymentsStartRequestedEvent(Object source) {
        super(source);
    }
}
