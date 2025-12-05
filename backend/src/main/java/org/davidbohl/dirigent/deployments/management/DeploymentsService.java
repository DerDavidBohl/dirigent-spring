package org.davidbohl.dirigent.deployments.management;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.davidbohl.dirigent.deployments.config.DeploymentsConfigurationProvider;
import org.davidbohl.dirigent.deployments.events.AllDeploymentsStartRequestedEvent;
import org.davidbohl.dirigent.deployments.events.DeploymentStateEvent;
import org.davidbohl.dirigent.deployments.events.MultipleNamedDeploymentsStartRequestedEvent;
import org.davidbohl.dirigent.deployments.events.NamedDeploymentStartRequestedEvent;
import org.davidbohl.dirigent.deployments.events.NamedDeploymentStopRequestedEvent;
import org.davidbohl.dirigent.deployments.events.RecreateAllDeploymentStatesEvent;
import org.davidbohl.dirigent.deployments.events.SourceDeploymentStartRequestedEvent;
import org.davidbohl.dirigent.deployments.models.Deployment;
import org.davidbohl.dirigent.deployments.models.DeploynentConfiguration;
import org.davidbohl.dirigent.deployments.state.DeploymentState;
import org.davidbohl.dirigent.deployments.state.DeploymentStatePersistingService;
import org.davidbohl.dirigent.sercrets.SecretService;
import org.davidbohl.dirigent.utility.git.GitService;
import org.davidbohl.dirigent.utility.process.ProcessResult;
import org.davidbohl.dirigent.utility.process.ProcessRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor()
public class DeploymentsService {

    public static final String DEPLOYMENTS_DIR_NAME = "deployments";
    private final GitService gitService;
    private final DeploymentsConfigurationProvider deploymentsConfigurationProvider;
    private final Logger logger = LoggerFactory.getLogger(DeploymentsService.class);
    private final ApplicationEventPublisher applicationEventPublisher;
    private final DeploymentStatePersistingService deploymentStatePersistingService;
    private final SecretService secretService;
    private final ProcessRunner processRunner;

    @Value("${dirigent.compose.command}")
    private String composeCommand;

    @EventListener(AllDeploymentsStartRequestedEvent.class)
    public void onAllDeploymentsStartRequested(AllDeploymentsStartRequestedEvent event) {

        makeDeploymentsDir();

        DeploynentConfiguration deploymentsConfiguration = tryGetConfiguration();
        deployListOfDeployments(deploymentsConfiguration.deployments(), event.isForceRecreate());
        stopNotConfiguredDeployments(deploymentsConfiguration.deployments());

    }

    private static void makeDeploymentsDir() {
        if (!new File(DEPLOYMENTS_DIR_NAME).mkdirs() && !new File(DEPLOYMENTS_DIR_NAME).exists())
            throw new DeploymentsDirCouldNotBeCreatedException();
    }

    @EventListener(MultipleNamedDeploymentsStartRequestedEvent.class)
    public void onMultipleNamedDeploymentsStartRequested(MultipleNamedDeploymentsStartRequestedEvent event) {
        makeDeploymentsDir();
        DeploynentConfiguration deploynentConfiguration = tryGetConfiguration();


        List<DeploymentState> deploymentStates = deploymentStatePersistingService.getDeploymentStates();
        List<DeploymentState> relevantDeploymentStates = deploymentStates.stream()
                .filter(ds -> event.getNames().stream().anyMatch(n -> n.equals(ds.getName()) &&
                        (ds.getState() != DeploymentState.State.STOPPED && ds.getState() != DeploymentState.State.REMOVED)))
                .toList();

        List<Deployment> deployments = deploynentConfiguration.deployments().stream().filter(
                d -> relevantDeploymentStates.stream().anyMatch(ds -> Objects.equals(ds.getName(), d.name()))
        ).toList();

        for (Deployment deployment : deployments) {
            deploy(deployment, event.isForceRecreate());
        }


    }

    @EventListener(NamedDeploymentStartRequestedEvent.class)
    public void onNamedDeploymentStartRequested(NamedDeploymentStartRequestedEvent event) {
        makeDeploymentsDir();
        DeploynentConfiguration deploynentConfiguration = tryGetConfiguration();

        Optional<Deployment> first = deploynentConfiguration.deployments().stream().filter(d -> Objects.equals(d.name(), event.getName())).findFirst();

        if (first.isEmpty())
            throw new DeploymentNameNotFoundException(event.getName());

        deploy(first.get(), event.isForceRecreate());
    }

    @EventListener(SourceDeploymentStartRequestedEvent.class)
    public void onSourceDeploymentStartRequested(SourceDeploymentStartRequestedEvent event) {
        makeDeploymentsDir();
        DeploynentConfiguration deploynentConfiguration = tryGetConfiguration();

        List<Deployment> deployments = deploynentConfiguration.deployments()
                .stream()
                .filter(d ->
                        Objects.equals(d.source(), event.getDeploymentSource()) &&
                        (d.ref() == null || Objects.equals(d.ref(), event.getRef()))
                )
                .collect(Collectors.toList());

        deployListOfDeployments(deployments, false);
    }

    @EventListener(NamedDeploymentStopRequestedEvent.class)
    public void onNamedDeploymentStopRequested(NamedDeploymentStopRequestedEvent event) throws IOException, InterruptedException {
        makeDeploymentsDir();
        stopDeployment(event.getName());
    }

    @EventListener(RecreateAllDeploymentStatesEvent.class)
    public void onRecreateAllDeploymentStatesEvent() {
        makeDeploymentsDir();
        DeploynentConfiguration deploynentConfiguration = tryGetConfiguration();

        stopNotConfiguredDeployments(deploynentConfiguration.deployments());
        List<String> stoppedDeployments = deploymentStatePersistingService.getDeploymentStates().stream()
                .filter(d -> d.getState() == DeploymentState.State.STOPPED)
                .map(DeploymentState::getName)
                .toList();
        stoppedDeployments.forEach(d -> {
            try {
                stopDeployment(d);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        List<Deployment> deployments = deploynentConfiguration.deployments().stream()
                .filter(d -> !stoppedDeployments.contains(d.name()))
                .toList();

        deployListOfDeployments(deployments, false);
    }

    private void deploy(Deployment deployment, boolean forceRecreate) {
        logger.info("Deploying {}", deployment.name());

        File deploymentDir = new File("deployments/" + deployment.name());

        try {

            String rev = deployment.ref() != null ? deployment.ref() : "HEAD";
            boolean updated = gitService.updateRepo(deployment.source(), deploymentDir.getAbsolutePath(), rev);
            Optional<DeploymentState> optionalState = deploymentStatePersistingService.getDeploymentStates().stream()
                    .filter(state -> state.getName().equals(deployment.name()))
                    .findFirst();

            boolean deployWouldChangeState = optionalState.isEmpty() || optionalState.get().getState() != DeploymentState.State.RUNNING;


            if (!updated && !forceRecreate && !deployWouldChangeState) {
                applicationEventPublisher.publishEvent(new DeploymentStateEvent(this, deployment.name(), DeploymentState.State.RUNNING, "Deployment '%s' successfully started".formatted(deployment.name())));
                logger.info("No update, forced recreation or changed states in deployment. Skipping {}", deployment.name());
                return;
            }

            if (updated) {
                applicationEventPublisher.publishEvent(new DeploymentStateEvent(this, deployment.name(), DeploymentState.State.UPDATED, "Deployment '%s' updated".formatted(deployment.name())));
            }
            applicationEventPublisher.publishEvent(new DeploymentStateEvent(this, deployment.name(), DeploymentState.State.STARTING, "Starting Deployment '%s'".formatted(deployment.name())));

            List<String> commandArgs = new java.util.ArrayList<>(Arrays.stream(composeCommand.split(" ")).toList());
            commandArgs.add("up");
            commandArgs.add("-d");
            commandArgs.add("--remove-orphans");

            if (forceRecreate || updated) {
                commandArgs.add("--force-recreate");
            }

            logger.info("Upping Compose for {}", deployment.name());

            ProcessResult composeUp = processRunner.executeCommand(commandArgs, 
                deploymentDir, 
                secretService.getAllSecretsAsEnvironmentVariableMapByDeployment(deployment.name()));
            
            if ((composeUp.exitCode() != 0)) {
                applicationEventPublisher.publishEvent(new DeploymentStateEvent(this, deployment.name(), DeploymentState.State.FAILED, composeUp.stderr()));
                return;
            }
        } catch (IOException | InterruptedException e) {
            applicationEventPublisher.publishEvent(new DeploymentStateEvent(this, deployment.name(), DeploymentState.State.FAILED, e.getMessage()));
            return;
        }

        applicationEventPublisher.publishEvent(new DeploymentStateEvent(this, deployment.name(), DeploymentState.State.RUNNING, "Deployment '%s' successfully started".formatted(deployment.name())));
    }

    private void stopNotConfiguredDeployments(List<Deployment> deployments) {
        logger.info("Stopping not configured deployments");
        File deploymentsDir = new File("deployments");
        File[] files = deploymentsDir.listFiles();

        if (files == null)
            return;

        for (File file : files) {
            if (file.isDirectory() && deployments.stream().noneMatch(d -> d.name().equals(file.getName()))) {
                try {
                    stopDeployment(file.getName());
                    deleteDirectory(file);
                    applicationEventPublisher.publishEvent(new DeploymentStateEvent(this, file.getName(), DeploymentState.State.REMOVED, "Deployment '%s' removed (Not configured)".formatted(file.getName())));
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        logger.info("Not configured deployments stopped");
    }

    private void stopDeployment(String deploymentName) throws InterruptedException, IOException {
        logger.info("Stopping deployment {}", deploymentName);


        Optional<DeploymentState> optionalState = deploymentStatePersistingService.getDeploymentStates().stream()
                .filter(state -> state.getName().equals(deploymentName))
                .findFirst();

        boolean stopWouldChangeState = optionalState.isEmpty() || optionalState.get().getState() != DeploymentState.State.STOPPED;

        if (stopWouldChangeState) {
            applicationEventPublisher.publishEvent(new DeploymentStateEvent(this, deploymentName, DeploymentState.State.STOPPING, "Stopping deployment '%s'".formatted(deploymentName)));
        }

        List<String> commandArgs = new ArrayList<>(Arrays.stream(composeCommand.split(" ")).toList());
        commandArgs.add("down");

        processRunner.executeCommand(commandArgs, new File(DEPLOYMENTS_DIR_NAME + "/" + deploymentName));
        applicationEventPublisher.publishEvent(new DeploymentStateEvent(this, deploymentName, DeploymentState.State.STOPPED, "Deployment '%s' stopped".formatted(deploymentName)));
    }

    private void deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        boolean deleted = directoryToBeDeleted.delete();

        if (!deleted)
            throw new RuntimeException("Could not delete directory " + directoryToBeDeleted);
    }

    private void deployListOfDeployments(List<Deployment> deployments, boolean forceRecreate) {

        makeDeploymentsDir();

        Map<Integer, List<Deployment>> deploymentsByOrder = deployments.stream()
                .collect(Collectors.groupingBy(Deployment::order));

        TreeMap<Integer, List<Deployment>> sortedDeployments = new TreeMap<>(deploymentsByOrder);

        try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            for (Integer orderGroupKey : sortedDeployments.keySet()) {

                logger.info("Starting deployments with order {}", orderGroupKey);

                List<Deployment> deploymentsOrderUnit = sortedDeployments.get(orderGroupKey);

                List<CompletableFuture<Void>> futures = new ArrayList<>();
                for (Deployment deployment : deploymentsOrderUnit) {
                    futures.add(CompletableFuture.runAsync(() -> deploy(deployment, forceRecreate), executorService));
                }

                try {
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                } catch (Throwable ex) {
                    throw new RuntimeException(ex);
                }


                logger.info("Deployments with order {} finished", orderGroupKey);
            }
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
