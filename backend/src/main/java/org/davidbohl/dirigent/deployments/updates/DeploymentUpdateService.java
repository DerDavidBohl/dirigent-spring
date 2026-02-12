package org.davidbohl.dirigent.deployments.updates;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.davidbohl.dirigent.deployments.config.DeploymentsConfigurationProvider;
import org.davidbohl.dirigent.deployments.config.model.Deployment;
import org.davidbohl.dirigent.deployments.updates.dto.DeploymentUpdateDto;
import org.davidbohl.dirigent.deployments.updates.entity.DeploymentUpdateEntity;
import org.davidbohl.dirigent.deployments.updates.event.DeploymentServiceImageUpdateFailedEvent;
import org.davidbohl.dirigent.deployments.updates.event.DeploymentServiceImageUpdatedEvent;
import org.davidbohl.dirigent.deployments.updates.event.ImageUpdateAvailableEvent;
import org.davidbohl.dirigent.deployments.updates.exception.CouldNotGetManifestDigestFromRegistryFailedException;
import org.davidbohl.dirigent.deployments.updates.model.DockerImage;
import org.davidbohl.dirigent.utility.process.ProcessRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeploymentUpdateService {

    private final ContainerRegistryClient containerRegistryClient;
    private final DeploymentsConfigurationProvider configurationProvider;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ProcessRunner processRunner;
    private final DeploymentUpdateRepository deploymentUpdateRepository;

    @Value("${dirigent.updates.disabled:false}")
    private boolean updatesDisabled;

    @Value("${dirigent.compose.command}")
    private String composeCommand;
    
    @Async
    public void updateDeployment(DeploymentUpdateDto deploymentUpdate) {

        List<DeploymentUpdateEntity> entities = markAsRunning(deploymentUpdate);

        try {
            String pullCommand = composeCommand + " pull " + deploymentUpdate.service();

            File deploymentDir = new File("deployments/" + deploymentUpdate.deploymentName());
            processRunner.executeCommand(Arrays.asList(pullCommand.split(" ")), deploymentDir);

            String upCommand = composeCommand + " up --remove-orphans -d " + deploymentUpdate.service();
            processRunner.executeCommand(Arrays.asList(upCommand.split(" ")), deploymentDir);

            this.applicationEventPublisher.publishEvent(
                new DeploymentServiceImageUpdatedEvent(this, deploymentUpdate.deploymentName(), deploymentUpdate.service(), deploymentUpdate.image())
            );

        } catch(Throwable e) {
            List<DeploymentUpdateEntity> notRunningEntities = entities.stream().map(due -> {due.setRunning(false); return due;}).toList();
            
            if(notRunningEntities != null)
                this.deploymentUpdateRepository.saveAll(notRunningEntities);

            log.warn("Failed to update deployment " + deploymentUpdate.deploymentName() + " service " + deploymentUpdate.service() + " image "+ deploymentUpdate.image(), e);
            this.applicationEventPublisher.publishEvent(
                new DeploymentServiceImageUpdateFailedEvent(this, deploymentUpdate.deploymentName(), deploymentUpdate.service(), deploymentUpdate.image(), e.getMessage())
            );
        }

        cleanupEntities(entities);
    }

    @Transactional
    List<DeploymentUpdateEntity> markAsRunning(DeploymentUpdateDto deploymentUpdate) {
        List<DeploymentUpdateEntity> entities = this.deploymentUpdateRepository.findAllByDeploymentNameAndServiceAndImage(deploymentUpdate.deploymentName(), deploymentUpdate.service(), deploymentUpdate.image());
        entities.forEach(e -> e.setRunning(true));
        return this.deploymentUpdateRepository.saveAll(entities);
    }
    
    @Transactional
    public void cleanupEntities(List<DeploymentUpdateEntity> entities) {
        this.deploymentUpdateRepository.deleteAll(entities);
    }

    @Scheduled(fixedRateString = "${dirigent.update.rate:3}", timeUnit = TimeUnit.HOURS)
    public void checkAllDeploymentForUpdates() {

        if (updatesDisabled)
            return;

        log.info("Checking For Updates");

        List<Deployment> deployments = configurationProvider.getConfiguration().deployments();

        for (Deployment deployment : deployments) {
            checkIfImageUpdatesExistForDeployment(deployment);
        }
    }

    @Async
    public void checkIfImageUpdatesExistForDeployment(Deployment deployment) {

        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();

        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();

        DockerClient dockerClient = DockerClientImpl.getInstance(config, httpClient);

        String labelKey = "com.docker.compose.project";

        List<Container> containers = dockerClient.listContainersCmd()
                .withShowAll(true)
                .withLabelFilter(List.of(labelKey + "=" + deployment.name())) // Filter by label
                .exec();

        for (Container container : containers) {

            DockerImage image = parseDockerImage(container.getImage());

            try {
                String registryDigest = this.containerRegistryClient.getRegistryDigest(image.registryEndpoint(),
                        image.image(), image.tag());

                if (registryDigest.equals(container.getImageId()))
                continue;

                String service = container.getLabels().getOrDefault("com.docker.compose.service", "unknown");

                log.info("Update Available for {}", container.getImage());

                applicationEventPublisher
                        .publishEvent(
                                new ImageUpdateAvailableEvent(this, deployment.name(), container.getImage(), service));

                List<DeploymentUpdateEntity> deploymentUpdates = deploymentUpdateRepository
                        .findAllByDeploymentNameAndServiceAndImage(deployment.name(), service, container.getImage());

                if (deploymentUpdates.size() > 0)
                    return;

                deploymentUpdateRepository
                        .save(new DeploymentUpdateEntity(null, deployment.name(), service, container.getImage(), false));

            } catch (CouldNotGetManifestDigestFromRegistryFailedException e) {
                log.warn("could not get digest from registry for image {}", image);
                log.warn("Could Not Get Manifest Digest From Registry", e);
            }

        }
    }

    @EventListener
    @Transactional
    public void deleteAllUpdatesFromDatabseOnStrtUp(ContextRefreshedEvent event) {
        log.info("Cleaned update repository");
        deploymentUpdateRepository.deleteAllByIsRunning(true);
    }

    private DockerImage parseDockerImage(String imageRef) {

        // Parse image reference (e.g., "quay.io/prometheus/node-exporter:latest")
        String[] parts = imageRef.split("/", 2);
        String registryDomain = parts.length > 1 && parts[0].contains(".") ? parts[0] : "docker.io";
        String remainder = parts.length > 1 && parts[0].contains(".") ? parts[1] : imageRef;

        // Normalize registry URL (docker.io → registry-1.docker.io)
        String registryUrl = normalizeRegistryUrl(registryDomain);
        String imagePath = normalizeImagePath(remainder, registryDomain.equals("docker.io"));
        String tag = extractTag(remainder);

        return new DockerImage(registryUrl, imagePath, tag);
    }

    private String normalizeRegistryUrl(String registryDomain) {
        return registryDomain.equals("docker.io")
                ? "https://registry-1.docker.io/v2/"
                : "https://" + registryDomain + "/v2/";
    }

    private String normalizeImagePath(String remainder, boolean isDockerHub) {
        String[] parts = remainder.split(":");
        String path = parts[0];
        if (isDockerHub && !path.contains("/")) {
            return "library/" + path; // Docker Hub defaults to "library/" for official images
        }
        return path;
    }

    private String extractTag(String remainder) {
        return remainder.contains(":") ? remainder.split(":")[1] : "latest";
    }

    public List<DeploymentUpdateDto> getDeploymentUpdates() {
        
        List<DeploymentUpdateEntity> allDeploymentEntities = this.deploymentUpdateRepository.findAll();

        return allDeploymentEntities.stream()
                .map(e -> new DeploymentUpdateDto(e.getDeploymentName(), e.getService(), e.getImage(), e.isRunning()))
                .toList();

    }

}
