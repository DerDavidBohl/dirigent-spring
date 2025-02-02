package org.davidbohl.dirigent.deployments.controller;

import org.davidbohl.dirigent.deployments.models.GiteaRequestBody;
import org.davidbohl.dirigent.deployments.models.events.AllDeploymentsStartRequestedEvent;
import org.davidbohl.dirigent.deployments.models.events.SourceDeploymentStartRequestedEvent;
import org.davidbohl.dirigent.deployments.service.DeploymentsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping("/api/v1/gitea")
public class GiteaDeploymentsController {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Value("${dirigent.deployments.git.url}")
    private String configUrl;

    public GiteaDeploymentsController(DeploymentsService deploymentsService, ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @PostMapping()
    public void webHook(@RequestBody GiteaRequestBody body) {

        if(body.repository().cloneUrl().equals(configUrl)) {
            applicationEventPublisher.publishEvent(new AllDeploymentsStartRequestedEvent(this, false));
            return;
        }

        applicationEventPublisher.publishEvent(new SourceDeploymentStartRequestedEvent(this, body.repository().cloneUrl()));
    }

}
