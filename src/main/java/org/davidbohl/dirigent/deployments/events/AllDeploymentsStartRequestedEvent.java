package org.davidbohl.dirigent.deployments.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class AllDeploymentsStartRequestedEvent extends ApplicationEvent {

    private final boolean forceRun;
    private final boolean forceRecreate;

    public AllDeploymentsStartRequestedEvent(Object source, boolean forceRun, boolean forceRecreate) {
        super(source);
        this.forceRun = forceRun;
        this.forceRecreate = forceRecreate;
    }
}
