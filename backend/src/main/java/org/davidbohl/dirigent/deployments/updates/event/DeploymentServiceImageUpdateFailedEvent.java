package org.davidbohl.dirigent.deployments.updates.event;

import org.springframework.context.ApplicationEvent;

import lombok.Getter;

@Getter
public class DeploymentServiceImageUpdateFailedEvent extends ApplicationEvent {

    private String deploymentName;
    private String service;
    private String image;
    private String message;

    public DeploymentServiceImageUpdateFailedEvent(Object source, String deploymentName, String service, String image, String message) {
        super(source);
        this.deploymentName = deploymentName;
        this.service = service;
        this.image = image;
        this.message = message;
    }
}