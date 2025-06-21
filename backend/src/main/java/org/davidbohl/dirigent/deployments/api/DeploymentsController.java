package org.davidbohl.dirigent.deployments.api;

import org.davidbohl.dirigent.deployments.events.AllDeploymentsStartRequestedEvent;
import org.davidbohl.dirigent.deployments.events.NamedDeploymentStartRequestedEvent;
import org.davidbohl.dirigent.deployments.events.NamedDeploymentStopRequestedEvent;
import org.davidbohl.dirigent.deployments.management.DeploymentNameNotFoundException;
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

    @PostMapping("/{name}/stop")
    public void stopDeployment(@PathVariable String name) {
        applicationEventPublisher.publishEvent(new NamedDeploymentStopRequestedEvent(this, name));
    }

    @PostMapping("/all/start")
    public void startAllDeployments(@RequestParam(required = false) boolean force,
                                    @RequestParam(required = false) boolean forceRun,
                                    @RequestParam(required = false) boolean forceRecreate) {
        applicationEventPublisher.publishEvent(new AllDeploymentsStartRequestedEvent(this,
                force || forceRun,
                force || forceRecreate));
    }

    @ExceptionHandler(DeploymentNameNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleDeploymentNotFound(DeploymentNameNotFoundException exception) {

        ProblemDetail body = ProblemDetail.forStatus(404);
        body.setDetail(exception.getMessage());
        return ResponseEntity.of(body).build();
    }
}
