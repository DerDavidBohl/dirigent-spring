package org.davidbohl.dirigent.deployments.controller;

import org.davidbohl.dirigent.deployments.service.DeploymentNameNotFoundException;
import org.davidbohl.dirigent.deployments.config.DeploymentsConfigurationProvider;
import org.davidbohl.dirigent.deployments.service.DeploymentsService;
import org.davidbohl.dirigent.deployments.models.DeploynentConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController()
@RequestMapping(path = "/api/v1/deployments")
public class DeploymentsController {
    private final DeploymentsConfigurationProvider deploymentsConfigurationProvider;
    private final DeploymentsService deploymentsService;
    private static final Logger logger = LoggerFactory.getLogger(DeploymentsController.class);

    public DeploymentsController(DeploymentsConfigurationProvider deploymentsConfigurationProvider, DeploymentsService deploymentsService) {
        this.deploymentsConfigurationProvider = deploymentsConfigurationProvider;
        this.deploymentsService = deploymentsService;
    }

    @GetMapping
    public DeploynentConfiguration getDeployments() throws IOException, InterruptedException {
        logger.info("Getting deployments");
        return deploymentsConfigurationProvider.getConfiguration();
    }

    @PostMapping("/{name}/start")
    public void startDeployment(String name) {
        deploymentsService.startSingleDeploymentByName(name);
    }

    @PostMapping("/all/start")
    public void startAllDeployments() {
        deploymentsService.startAllDeployments();
    }

    @ExceptionHandler(DeploymentNameNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleDeploymentNotFound(DeploymentNameNotFoundException exception) {

        ProblemDetail body = ProblemDetail.forStatus(404);
        body.setDetail(exception.getMessage());
        return ResponseEntity.of(body).build();
    }
}
