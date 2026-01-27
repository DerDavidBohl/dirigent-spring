package org.davidbohl.dirigent.deployments.updates.event;

import org.springframework.context.ApplicationEvent;

import lombok.Getter;

@Getter
public class DeploymentServiceImageUpdatedEvent extends ApplicationEvent {

    private String deploymentName;
    private String service;
    private String image;

    public DeploymentServiceImageUpdatedEvent(Object source, String deploymentName, String service, String image) {
        super(source);
        this.deploymentName = deploymentName;
        this.service = service;
        this.image = image;
    }
}