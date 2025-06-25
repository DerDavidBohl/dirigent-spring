package org.davidbohl.dirigent.deployments.api;

import org.davidbohl.dirigent.deployments.config.DeploymentsConfigurationProvider;
import org.davidbohl.dirigent.deployments.events.AllDeploymentsStartRequestedEvent;
import org.davidbohl.dirigent.deployments.events.NamedDeploymentStartRequestedEvent;
import org.davidbohl.dirigent.deployments.events.NamedDeploymentStopRequestedEvent;
import org.davidbohl.dirigent.deployments.management.DeploymentNameNotFoundException;
import org.davidbohl.dirigent.deployments.models.DeploynentConfiguration;
import org.davidbohl.dirigent.deployments.state.DeploymentState;
import org.davidbohl.dirigent.deployments.state.DeploymentStatePersistingService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController()
@RequestMapping(path = "/api/v1/deployments")
public class DeploymentsController {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final DeploymentsConfigurationProvider deploymentsConfigurationProvider;
    private final DeploymentStatePersistingService deploymentStatePersistingService;

    public DeploymentsController(ApplicationEventPublisher applicationEventPublisher, DeploymentsConfigurationProvider deploymentsConfigurationProvider, DeploymentStatePersistingService deploymentStateRepository) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.deploymentsConfigurationProvider = deploymentsConfigurationProvider;
        this.deploymentStatePersistingService = deploymentStateRepository;
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

    @GetMapping()
    public List<DeploymentDto> getDeployments() throws IOException, InterruptedException {
        DeploynentConfiguration configuration = this.deploymentsConfigurationProvider.getConfiguration();
        List<DeploymentState> deploymentStates = this.deploymentStatePersistingService.getDeploymentStates();

        return configuration.deployments().stream()
                .map(deployment -> new DeploymentDto(
                        deployment.name(),
                        deployment.source(),
                        deployment.order(),
                        deploymentStates.stream()
                                .filter(state -> state.getName().equals(deployment.name()))
                                .findFirst()
                                .map(DeploymentState::getState)
                                .orElse(DeploymentState.State.UNKNOWN)))
                .toList();


    }

    @ExceptionHandler(DeploymentNameNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleDeploymentNotFound(DeploymentNameNotFoundException exception) {

        ProblemDetail body = ProblemDetail.forStatus(404);
        body.setDetail(exception.getMessage());
        return ResponseEntity.of(body).build();
    }
}
