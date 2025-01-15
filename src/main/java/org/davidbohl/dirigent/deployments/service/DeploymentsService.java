package org.davidbohl.dirigent.deployments.service;

import org.davidbohl.dirigent.deployments.config.DeploymentsConfigurationProvider;
import org.davidbohl.dirigent.deployments.models.Deployment;
import org.davidbohl.dirigent.deployments.models.DeploynentConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;
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

        DeploynentConfiguration deploymentsConfiguration = tryGetConfiguration();

        deployAll(deploymentsConfiguration.deployments());

        stopNotConfiguredDeployments(deploymentsConfiguration.deployments());
    }

    private void stopNotConfiguredDeployments(List<Deployment> deployments) {
        File deploymentsDir = new File("deployments");
        File[] files = deploymentsDir.listFiles();

        if(files == null)
            return;

        for (File file : files) {
            if (file.isDirectory() && !deployments.stream().anyMatch(d -> d.name().equals(file.getName()))) {
                try {
                    new ProcessBuilder(composeCommand, "down")
                            .directory(file.getAbsoluteFile())
                            .start()
                            .waitFor();
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void deployAll(List<Deployment> deployments) {

        new File("deployments").mkdirs();

        Map<Integer, List<Deployment>> deploymentsByOrder = deployments.stream()
                .collect(Collectors.groupingBy(Deployment::order));

        TreeMap<Integer, List<Deployment>> sortedDeployments = new TreeMap<>(deploymentsByOrder);

        for (Integer orderGroupKey : sortedDeployments.keySet()) {

            logger.info("Starting deployments with order {}", orderGroupKey);

            List<Deployment> deploymentsOrderUnit = sortedDeployments.get(orderGroupKey);
            ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

            for (Deployment deployment : deploymentsOrderUnit) {
                executorService.submit(() -> deploy(deployment));
            }

            executorService.shutdown();
            try {
                executorService.awaitTermination(Long.MAX_VALUE, java.util.concurrent.TimeUnit.NANOSECONDS);
            } catch (Throwable ex) {
                throw new RuntimeException(ex);
            }


            logger.info("Deployments with order {} finished", orderGroupKey);
        }
    }

    public void startSingleDeploymentByName(String name) {
        new File("deployments").mkdirs();
        DeploynentConfiguration deploynentConfiguration = tryGetConfiguration();

        Optional<Deployment> first = deploynentConfiguration.deployments().stream().filter(d -> d.name() == name).findFirst();

        if(first.isEmpty())
            throw new DeploymentNameNotFoundException(name);

        deploy(first.get());
    }

    public void startSingleDeploymentBySource(String source) {
        new File("deployments").mkdirs();
        DeploynentConfiguration deploynentConfiguration = tryGetConfiguration();

        List<Deployment> deployments = deploynentConfiguration.deployments()
                .stream()
                .filter(d -> Objects.equals(d.source(), source))
                .collect(Collectors.toList());

        deployAll(deployments);
    }

    private DeploynentConfiguration tryGetConfiguration() {

        try {
            return deploymentsConfigurationProvider.getConfiguration();
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
