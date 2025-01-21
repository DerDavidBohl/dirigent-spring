package org.davidbohl.dirigent.deployments.service;

import org.davidbohl.dirigent.deployments.config.DeploymentsConfigurationProvider;
import org.davidbohl.dirigent.deployments.models.Deployment;
import org.davidbohl.dirigent.deployments.models.DeploynentConfiguration;
import org.davidbohl.dirigent.deployments.models.events.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class DeploymentsService {

    private final GitService gitService;
    private final DeploymentsConfigurationProvider deploymentsConfigurationProvider;
    private final Logger logger = LoggerFactory.getLogger(DeploymentsService.class);
    private final ApplicationEventPublisher applicationEventPublisher;

    @Value("${dirigent.compose.command}")
    private String composeCommand;

    public DeploymentsService(
            DeploymentsConfigurationProvider deploymentsConfigurationProvider,
            GitService gitService, ApplicationEventPublisher applicationEventPublisher) {
        this.deploymentsConfigurationProvider = deploymentsConfigurationProvider;
        this.gitService = gitService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @EventListener(AllDeploymentsStartRequestedEvent.class)
    public void onAllDeploymentsStartRequested() {

        new File("deployments").mkdirs();

        DeploynentConfiguration deploymentsConfiguration = tryGetConfiguration();
        deployListOfDeployments(deploymentsConfiguration.deployments());
        stopNotConfiguredDeployments(deploymentsConfiguration.deployments());

    }

    @EventListener(NamedDeploymentStartRequestedEvent.class)
    public void onNamedDeploymentStartRequested(NamedDeploymentStartRequestedEvent event) {
        new File("deployments").mkdirs();
        DeploynentConfiguration deploynentConfiguration = tryGetConfiguration();

        Optional<Deployment> first = deploynentConfiguration.deployments().stream().filter(d -> Objects.equals(d.name(), event.getName())).findFirst();

        if(first.isEmpty())
            throw new DeploymentNameNotFoundException(event.getName());

        deploy(first.get());
    }

    @EventListener(SourceDeploymentStartRequestedEvent.class)
    public void onSourceDeploymentStartRequested(SourceDeploymentStartRequestedEvent event) {
        new File("deployments").mkdirs();
        DeploynentConfiguration deploynentConfiguration = tryGetConfiguration();

        List<Deployment> deployments = deploynentConfiguration.deployments()
                .stream()
                .filter(d -> Objects.equals(d.source(), event.getDeploymentSource()))
                .collect(Collectors.toList());

        deployListOfDeployments(deployments);
    }

    private void deploy(Deployment deployment) {
        logger.info("Deploying {}", deployment.name());

        File deploymentDir = new File("deployments/" + deployment.name());

        try {
            boolean updated = gitService.updateRepo(deployment.source(), deploymentDir.getAbsolutePath());

            if(!updated) {
                logger.info("No changes in deployment. Skipping {}", deployment.name());
                return;
            }

            List<String> commandArgs = new java.util.ArrayList<>(Arrays.stream(composeCommand.split(" ")).toList());
            commandArgs.add("up");
            commandArgs.add("-d");
            commandArgs.add("--remove-orphans");

            Process process = new ProcessBuilder(commandArgs)
                    .directory(deploymentDir)
                    .start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line + "\n");
            }

            int exitVal = process.waitFor();
            if (exitVal != 0) {
                applicationEventPublisher.publishEvent(new DeploymentStartFailedEvent(this, deployment.name(), output.toString()));
            }
        } catch (IOException | InterruptedException e) {
            applicationEventPublisher.publishEvent(new DeploymentStartFailedEvent(this, deployment.name(), e.getMessage()));
            return;
        }

        applicationEventPublisher.publishEvent(new DeploymentStartSucceededEvent(this, deployment.name()));
    }

    private void stopNotConfiguredDeployments(List<Deployment> deployments) {
        logger.info("Stopping not configured deployments");
        File deploymentsDir = new File("deployments");
        File[] files = deploymentsDir.listFiles();

        if(files == null)
            return;

        for (File file : files) {
            if (file.isDirectory() && deployments.stream().noneMatch(d -> d.name().equals(file.getName()))) {
                try {
                    logger.info("Stopping deployment {}", file.getName());
                    List<String> commandArgs = new java.util.ArrayList<>(Arrays.stream(composeCommand.split(" ")).toList());
                    commandArgs.add("down");
                    new ProcessBuilder(commandArgs)
                            .directory(file)
                            .start()
                            .waitFor();
                    deleteDirectory(file);
                    applicationEventPublisher.publishEvent(new NotConfiguredDeploymentStopped(this, file.getName()));
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        logger.info("Not configured deployments stopped");
    }

    void deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        directoryToBeDeleted.delete();
    }

    private void deployListOfDeployments(List<Deployment> deployments) {

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

    private DeploynentConfiguration tryGetConfiguration() {

        try {
            return deploymentsConfigurationProvider.getConfiguration();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
