package org.davidbohl.dirigent.deployments.notification;

import lombok.extern.slf4j.Slf4j;

import org.davidbohl.dirigent.deployments.state.event.DeploymentStateChangedEvent;
import org.davidbohl.dirigent.deployments.updates.event.ImageUpdateAvailableEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
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

    private void sendGotifyMessage(String title, String message) {
        if (gotifyToken != null && gotifyBaseUrl != null && !gotifyToken.isBlank() && !gotifyBaseUrl.isBlank()) {
            RestTemplate restTemplate = new RestTemplate();
            restTemplate.postForObject("%s/message?token=%s".formatted(gotifyBaseUrl, gotifyToken), new GotifyMessage(title, message, 5), Object.class);
        }
    }

    record GotifyMessage(String title, String message, int priority) {
    }
}
