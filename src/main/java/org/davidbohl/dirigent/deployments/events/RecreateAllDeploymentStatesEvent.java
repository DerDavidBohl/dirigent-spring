package org.davidbohl.dirigent.deployments.events;

import org.springframework.context.ApplicationEvent;

public class RecreateAllDeploymentStatesEvent extends ApplicationEvent {
    public RecreateAllDeploymentStatesEvent(Object source) {
        super(source);
    }
}
