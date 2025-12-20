package org.davidbohl.dirigent.deployments.management;

import java.util.List;
import java.util.Optional;

import org.davidbohl.dirigent.deployments.config.CachedDeploymentsConfigurationProvider;
import org.davidbohl.dirigent.deployments.config.model.DeploynentConfiguration;
import org.davidbohl.dirigent.deployments.management.event.AllDeploymentsStartRequestedEvent;
import org.davidbohl.dirigent.deployments.management.event.NamedDeploymentStartRequestedEvent;
import org.davidbohl.dirigent.deployments.management.event.NamedDeploymentStopRequestedEvent;
import org.davidbohl.dirigent.deployments.management.exception.DeploymentNameNotFoundException;
import org.davidbohl.dirigent.deployments.management.model.DeploymentDto;
import org.davidbohl.dirigent.deployments.state.DeploymentStatePersistingService;
import org.davidbohl.dirigent.deployments.state.entity.DeploymentStateEntity;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping(path = "/api/v1/deployments")
public class DeploymentsController {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final CachedDeploymentsConfigurationProvider deploymentsConfigurationProvider;
    private final DeploymentStatePersistingService deploymentStatePersistingService;

    public DeploymentsController(ApplicationEventPublisher applicationEventPublisher,
                                 CachedDeploymentsConfigurationProvider deploymentsConfigurationProvider,
                                 DeploymentStatePersistingService deploymentStateRepository) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.deploymentsConfigurationProvider = deploymentsConfigurationProvider;
        this.deploymentStatePersistingService = deploymentStateRepository;
    }

    @PostMapping("/{name}/start")
    public void startDeployment(@PathVariable String name, @RequestParam(required = false) boolean forceRecreate) {
        applicationEventPublisher.publishEvent(new NamedDeploymentStartRequestedEvent(this, name, forceRecreate));
    }

    @PostMapping("/{name}/stop")
    public void stopDeployment(@PathVariable String name) {
        applicationEventPublisher.publishEvent(new NamedDeploymentStopRequestedEvent(this, name));
    }

    @PostMapping("/all/start")
    public void startAllDeployments(@RequestParam(required = false) boolean forceRecreate) {
        applicationEventPublisher.publishEvent(new AllDeploymentsStartRequestedEvent(this, forceRecreate));
    }

    @GetMapping()
    public List<DeploymentDto> getDeployments() {
        DeploynentConfiguration configuration = this.deploymentsConfigurationProvider.getCachedConfiguration();
        List<DeploymentStateEntity> deploymentStates = this.deploymentStatePersistingService.getDeploymentStates();

        return configuration.deployments().stream()
                .map(deployment -> {

                    Optional<DeploymentStateEntity> state = deploymentStates.stream()
                            .filter(ds -> ds.getName().equals(deployment.name()))
                            .findFirst();

                    return new DeploymentDto(
                            deployment.name(),
                            deployment.source(),
                            deployment.order(),
                            state.isPresent() ? state.get().getState() : DeploymentStateEntity.State.UNKNOWN,
                            state.isPresent() ? state.get().getMessage() : ""
                    );
                })
                .toList();


    }

    @ExceptionHandler(DeploymentNameNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleDeploymentNotFound(DeploymentNameNotFoundException exception) {

        ProblemDetail body = ProblemDetail.forStatus(404);
        body.setDetail(exception.getMessage());
        return ResponseEntity.of(body).build();
    }
}
