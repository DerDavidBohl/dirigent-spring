package org.davidbohl.dirigent.deployments.updates;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.davidbohl.dirigent.deployments.config.DeploymentsConfigurationProvider;
import org.davidbohl.dirigent.deployments.events.ImageUpdateAvailableEvent;
import org.davidbohl.dirigent.deployments.models.Deployment;
import org.springframework.context.ApplicationEventPublisher;
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

    @Scheduled(fixedRateString = "${dirigent.update.rate:30}", timeUnit = TimeUnit.SECONDS)
    public void checkAllDeploymentForUpdates() {

        log.info("Checking For Updates");

        List<Deployment> deployments = configurationProvider.getConfiguration().deployments();

        for (Deployment deployment : deployments) {
            checkIfImageUpdatesExistForDeployment(deployment);
        }
    }

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

    private DockerImage parseDockerImage(String imageString) {

        // TODO: Move this to config
        String host = "registry-1.docker.io";
        String tag = "latest";

        String[] parts = imageString.split("/");

        if (parts[0].contains(".") || parts[0].matches(".*:\\d+")) {
            host = parts[0];
        }

        int slashIndex = imageString.indexOf('/');

        String image = (slashIndex != -1) ? imageString.substring(slashIndex + 1) : "library/" + imageString;

        if (image.contains(":")) {
            tag = image.split(":")[1];
            image = image.split(":")[0];
        }

        return new DockerImage(host, image, tag);
    }

}
