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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
                    .collect(Collectors.groupingBy(Deployment::order));

            TreeMap<Integer, List<Deployment>> sortedDeployments = new TreeMap<>(deploymentsByOrder);

            for (Integer orderGroupKey : sortedDeployments.keySet()) {

                logger.info("Starting deployments with order {}", orderGroupKey);

                List<Deployment> deployments = sortedDeployments.get(orderGroupKey);
                ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

                for (Deployment deployment : deployments) {
                    executorService.submit(() -> deploy(deployment));
                }

                executorService.shutdown();
                executorService.awaitTermination(Long.MAX_VALUE, java.util.concurrent.TimeUnit.NANOSECONDS);

                logger.info("Deployments with order {} finished", orderGroupKey);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private void deploy(Deployment deployment) {
        logger.info("Deploying {}", deployment.name());

        File deploymentDir = new File("deployments/" + deployment.name());

        try {
        gitService.cloneOrPull(deployment.source(), deploymentDir.getAbsolutePath());

            List<String> commandArgs = new java.util.ArrayList<>(Arrays.stream(composeCommand.split(" ")).toList());
            commandArgs.add("up");
            commandArgs.add("-d");
            commandArgs.add("--remove-orphans");

            new ProcessBuilder(commandArgs)
                .directory(deploymentDir)
                .start().waitFor();

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        logger.info("Deployment {} started", deployment.name());
    }

}
