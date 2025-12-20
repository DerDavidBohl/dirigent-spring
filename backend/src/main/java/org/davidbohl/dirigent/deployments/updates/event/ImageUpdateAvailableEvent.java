package org.davidbohl.dirigent.deployments.updates.event;

import org.springframework.context.ApplicationEvent;

import lombok.Getter;

@Getter
public class ImageUpdateAvailableEvent extends ApplicationEvent {

    private String deploymentName;
    private String serviceName;
    private String image;

    public ImageUpdateAvailableEvent(Object source, String deploymentName, String image, String serviceName) {
        super(source);
        this.deploymentName = deploymentName;
        this.image = image;
        this.serviceName = serviceName;
    }
}
