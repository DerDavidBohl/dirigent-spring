package org.davidbohl.dirigent.deployments.api;

import org.davidbohl.dirigent.deployments.state.DeploymentState;
import org.davidbohl.dirigent.deployments.state.DeploymentStatePersistingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController()
@RequestMapping(path = "/api/v1/deployment-states")
public class DeploymentStatesController {

    private final DeploymentStatePersistingService deploymentStatePersistingService;

    public DeploymentStatesController(DeploymentStatePersistingService deploymentStatePersistingService) {
        this.deploymentStatePersistingService = deploymentStatePersistingService;
    }

    @GetMapping
    public List<DeploymentState> getDeploymentStates() {
        return deploymentStatePersistingService.getDeploymentStates();
    }

}
