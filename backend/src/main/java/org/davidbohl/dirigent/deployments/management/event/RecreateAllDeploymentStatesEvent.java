package org.davidbohl.dirigent.deployments.management.event;

import org.springframework.context.ApplicationEvent;

public class RecreateAllDeploymentStatesEvent extends ApplicationEvent {
    public RecreateAllDeploymentStatesEvent(Object source) {
        super(source);
    }
}
