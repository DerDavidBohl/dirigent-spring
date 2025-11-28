package org.davidbohl.dirigent.deployments.updates;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.davidbohl.dirigent.deployments.config.DeploymentsConfigurationProvider;
import org.davidbohl.dirigent.deployments.events.ImageUpdateAvailableEvent;
import org.davidbohl.dirigent.deployments.models.Deployment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
public class DeploymentUpdateService {

    private final ContainerRegistryClient containerRegistryClient;
    private final DeploymentsConfigurationProvider configurationProvider;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Value("${dirigent.update.enabled:false}")
    boolean updateEnabled;

    @Scheduled(fixedRateString = "${dirigent.update.rate:30}", timeUnit = TimeUnit.SECONDS)
    public void checkAllDeploymentForUpdates() {

        if(!updateEnabled)
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
                                .withLabelFilter(List.of(labelKey + "=" + deployment.name()))  // Filter by label
                                .exec();


        for (Container container : containers) {
            
            DockerImage image = parseDockerImage(container.getImage());

            try {
                String registryDigest = this.containerRegistryClient.getRegistryDigest(image.host(), image.image(), image.tag());

                // InspectImageResponse imageData = dockerClient.inspectImageCmd(container.getImageId()).exec();

                if(registryDigest.equals(container.getImageId()))
                    continue;

                log.info("Update Available for {}", container.getImage());
                applicationEventPublisher.publishEvent(new ImageUpdateAvailableEvent(this, deployment.name(), container.getImage(), container.getLabels().getOrDefault("com.docker.compose.service", "unknown")));

            } catch (CouldNotGetManifestDigestFromRegistryFailedException e) {
                log.warn("could not get digest from registry for image {}", image);
                log.warn("Could Not Get Manifest Digest From Registry", e);
            }

        }
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
            return "library/" + path;  // Docker Hub defaults to "library/" for official images
        }
        return path;
    }

    private String extractTag(String remainder) {
        return remainder.contains(":") ? remainder.split(":")[1] : "latest";
    }

}
