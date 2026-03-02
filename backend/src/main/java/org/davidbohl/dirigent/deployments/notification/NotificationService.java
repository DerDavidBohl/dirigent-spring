package org.davidbohl.dirigent.deployments.notification;

import lombok.extern.slf4j.Slf4j;

import org.davidbohl.dirigent.deployments.state.event.DeploymentStateChangedEvent;
import org.davidbohl.dirigent.deployments.updates.event.DeploymentServiceImageUpdateFailedEvent;
import org.davidbohl.dirigent.deployments.updates.event.DeploymentServiceImageUpdatedEvent;
import org.davidbohl.dirigent.deployments.updates.event.ImageUpdateAvailableEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class NotificationService {

    @Value("${dirigent.gotify.baseUrl:}")
    private String gotifyBaseUrl;

    @Value("${dirigent.gotify.token:}")
    private String gotifyToken;

    @EventListener(DeploymentStateChangedEvent.class)
    @Async
    public void onDeploymentStateChanged(DeploymentStateChangedEvent event) {
        String title = "%s: \"%s\"".formatted(event.getState(), event.getDeploymentName());
        String context = event.getContext();
        sendGotifyMessage(title, context);

        log.info("Deployment '{}' state changed to {}. Context: {}", event.getDeploymentName(), event.getState(), context);
    }

    @EventListener(ImageUpdateAvailableEvent.class)
    @Async
    public void onImageUpdateAvailable(ImageUpdateAvailableEvent event) {
        String title = "Image Update available: " + event.getImage();
        String message = "New version of image " + event.getImage() + " in deployment " + event.getDeploymentName() + " in service " + event.getServiceName() + " found.";

        sendGotifyMessage(title, message);
    }

    @EventListener(DeploymentServiceImageUpdatedEvent.class)
    @Async
    public void onDeploymentServiceImageUpdated(DeploymentServiceImageUpdatedEvent event) {
        String title = "Image updated";
        String message = "Deployment: " + event.getDeploymentName() + "\nService: " + event.getService() + "\nImage: " + event.getImage();

        sendGotifyMessage(title, message);
    }

    @EventListener(DeploymentServiceImageUpdateFailedEvent.class)
    @Async
    public void onDeploymentServiceImageUpdateFailed(DeploymentServiceImageUpdateFailedEvent event) {
        String title = "Image Update failed";
        String message = "Deployment: " + event.getDeploymentName() + "\nService: " + event.getService() + "\nImage: " + event.getImage() + "\nError:\n" + event.getMessage();

        sendGotifyMessage(title, message);
    }

    @Retryable
    private void sendGotifyMessage(String title, String message) {
        try {
            if (gotifyToken != null && gotifyBaseUrl != null && !gotifyToken.isBlank() && !gotifyBaseUrl.isBlank()) {
                RestTemplate restTemplate = new RestTemplate();
                restTemplate.postForObject("%s/message?token=%s".formatted(gotifyBaseUrl, gotifyToken), new GotifyMessage(title, message, 5), Object.class);
            }
        } catch(Throwable e) {
            log.warn("Failed to send Message to gotify", e);
        }
    }

    record GotifyMessage(String title, String message, int priority) {
    }
}
