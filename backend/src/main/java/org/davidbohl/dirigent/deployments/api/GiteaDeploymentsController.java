package org.davidbohl.dirigent.deployments.api;

import org.davidbohl.dirigent.deployments.events.RecreateAllDeploymentStatesEvent;
import org.davidbohl.dirigent.deployments.events.SourceDeploymentStartRequestedEvent;
import org.davidbohl.dirigent.deployments.models.GiteaRequestBody;
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

    @Value("${dirigent.deployments.git.url:}")
    private String configUrl;

    public GiteaDeploymentsController(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @PostMapping()
    public void webHook(@RequestBody GiteaRequestBody body) {

        if(body.repository().cloneUrl().equals(configUrl)) {
            applicationEventPublisher.publishEvent(new RecreateAllDeploymentStatesEvent(this));
            return;
        }

        String cleanRef = body.ref();

        if(cleanRef.contains("/")) {
            String[] refParts = body.ref().split("/");
            cleanRef = refParts[refParts.length - 1];
        }

        applicationEventPublisher.publishEvent(new SourceDeploymentStartRequestedEvent(this, body.repository().cloneUrl(), cleanRef));
    }

}
