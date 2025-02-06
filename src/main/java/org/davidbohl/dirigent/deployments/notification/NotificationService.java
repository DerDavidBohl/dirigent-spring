package org.davidbohl.dirigent.deployments.notification;

import org.davidbohl.dirigent.deployments.events.DeploymentStartFailedEvent;
import org.davidbohl.dirigent.deployments.events.DeploymentStartSucceededEvent;
import org.davidbohl.dirigent.deployments.events.NotConfiguredDeploymentStopped;
import org.davidbohl.dirigent.deployments.management.DeploymentsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class NotificationService {

    @Value("${dirigent.gotify.baseUrl:}")
    private String gotifyBaseUrl;

    @Value("${dirigent.gotify.token:}")
    private String gotifyToken;

    private final Logger logger = LoggerFactory.getLogger(DeploymentsService.class);

    @EventListener(DeploymentStartFailedEvent.class)
    public void onDeploymentStartFailed(DeploymentStartFailedEvent event) {
        sendGotifyMessage(event.getMessage(), "Deployment \"%s\" Failed".formatted(event.getDeploymentName()));

        logger.warn("Deployment '{}' failed. Error: {}", event.getDeploymentName(), event.getMessage());

    }

    @EventListener(DeploymentStartSucceededEvent.class)
    public void onDeploymentStartSucceeded(DeploymentStartSucceededEvent event) {
        sendGotifyMessage("Deployment succeeded", "Deployment \"%s\" Succeeded".formatted(event.getDeploymentName()));

        logger.info("Deployment '{}' succeeded.", event.getDeploymentName());
    }

    @EventListener(NotConfiguredDeploymentStopped.class)
    public void onNotConfiguredDeploymentStopped(NotConfiguredDeploymentStopped event) {
        sendGotifyMessage("Deployment stopped", "Deployment \"%s\" stopped because it is not configured".formatted(event.getDeploymentName()));

        logger.info("Deployment '{}' stopped because it is not configured.", event.getDeploymentName());
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
