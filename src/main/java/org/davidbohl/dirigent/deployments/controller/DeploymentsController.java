package org.davidbohl.dirigent.deployments.controller;

import org.davidbohl.dirigent.deployments.models.events.AllDeploymentsStartRequestedEvent;
import org.davidbohl.dirigent.deployments.models.events.NamedDeploymentStartRequestedEvent;
import org.davidbohl.dirigent.deployments.service.DeploymentNameNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController()
@RequestMapping(path = "/api/v1/deployments")
public class DeploymentsController {

    private final ApplicationEventPublisher applicationEventPublisher;

    public DeploymentsController(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @PostMapping("/{name}/start")
    public void startDeployment(@PathVariable String name, @RequestParam(required = false) boolean force) {
        applicationEventPublisher.publishEvent(new NamedDeploymentStartRequestedEvent(this, name, force));
    }

    @PostMapping("/all/start")
    public void startAllDeployments(@RequestParam(required = false) boolean force) {
        applicationEventPublisher.publishEvent(new AllDeploymentsStartRequestedEvent(this, force));
    }

    @ExceptionHandler(DeploymentNameNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleDeploymentNotFound(DeploymentNameNotFoundException exception) {

        ProblemDetail body = ProblemDetail.forStatus(404);
        body.setDetail(exception.getMessage());
        return ResponseEntity.of(body).build();
    }
}
