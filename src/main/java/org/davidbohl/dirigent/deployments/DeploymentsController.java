package org.davidbohl.dirigent.deployments;

import org.davidbohl.dirigent.deployments.models.DeploynentConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController()
@RequestMapping(path = "/deployments")
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

    @PostMapping("/all/start")
    public void startAllDeployments() {
        logger.info("Starting all deployments");
        deploymentsService.startAllDeployments();
        logger.info("Deployments started");
    }
}
