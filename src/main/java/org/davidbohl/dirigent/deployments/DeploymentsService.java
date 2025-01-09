package org.davidbohl.dirigent.deployments;

import org.davidbohl.dirigent.deployments.models.Deployment;
import org.davidbohl.dirigent.deployments.models.DeploynentConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DeploymentsService {

    private final DeploymentsConfigurationProvider deploymentsConfigurationProvider;
    private final GitService gitService;
    private final Logger logger = LoggerFactory.getLogger(DeploymentsService.class);

    @Value("${dirigent.compose.command}")
    private String composeCommand;

    public DeploymentsService(
            @Autowired DeploymentsConfigurationProvider deploymentsConfigurationProvider,
            @Autowired GitService gitService) {
        this.deploymentsConfigurationProvider = deploymentsConfigurationProvider;
        this.gitService = gitService;
    }

    public void startAllDeployments() {

        new File("deployments").mkdirs();

        try {
            DeploynentConfiguration deploymentsConfiguration = deploymentsConfigurationProvider.getConfiguration();

            Map<Integer, List<Deployment>> deploymentsByOrder = deploymentsConfiguration.deployments().stream()
                    .sorted(Comparator.comparingInt(Deployment::order))
                    .collect(Collectors.groupingBy(Deployment::order));

            for (Deployment deployment : deploymentsConfiguration.deployments()) {
                deploy(deployment);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private void deploy(Deployment deployment) throws IOException, InterruptedException {
        logger.info("Deploying {}", deployment.name());

        File deploymentDir = new File("deployments/" + deployment.name());

        gitService.cloneOrPull(deployment.source(), deploymentDir.getAbsolutePath());

        new ProcessBuilder(composeCommand, "up", "-d", "--remove-orphans")
                .directory(deploymentDir)
                .start().waitFor();

        logger.info("Deployment {} started", deployment.name());
    }

}
