package org.davidbohl.dirigent.deployments.management;

import org.davidbohl.dirigent.deployments.management.event.RecreateAllDeploymentStatesEvent;
import org.davidbohl.dirigent.deployments.management.event.SourceDeploymentStartRequestedEvent;
import org.davidbohl.dirigent.deployments.management.model.GiteaRequestBody;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping("/api/v1/gitea")
// SEC-004: Require a machine token dedicated to webhook triggering.
public class GiteaDeploymentsController {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Value("${dirigent.deployments.git.url:}")
    private String configUrl;

    public GiteaDeploymentsController(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    // REQ-004: Route configured repository events into deployment reconciliation.
    @PostMapping()
    @Secured("SCOPE_webhooks:trigger")
    public ResponseEntity<Void> webHook(@RequestBody GiteaRequestBody body) {
        if(body.repository().cloneUrl().equals(configUrl)) {
            applicationEventPublisher.publishEvent(new RecreateAllDeploymentStatesEvent(this));
            return ResponseEntity.ok().build();
        }

        String cleanRef = body.ref();

        if(cleanRef.contains("/")) {
            String[] refParts = body.ref().split("/");
            cleanRef = refParts[refParts.length - 1];
        }

        applicationEventPublisher.publishEvent(new SourceDeploymentStartRequestedEvent(this, body.repository().cloneUrl(), cleanRef));
        return ResponseEntity.ok().build();
    }

}
