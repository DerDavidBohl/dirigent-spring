package org.davidbohl.dirigent.deployments.management.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Getter
public class MultipleNamedDeploymentsStartRequestedEvent extends ApplicationEvent {
    private final List<String> names;
    private final boolean forceRecreate;

    public MultipleNamedDeploymentsStartRequestedEvent(Object source, List<String> names, boolean forceRecreate) {
        super(source);
        this.names = names;
        this.forceRecreate = forceRecreate;
    }
}
