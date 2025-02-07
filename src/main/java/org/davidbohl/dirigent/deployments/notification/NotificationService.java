package org.davidbohl.dirigent.deployments.notification;

import lombok.extern.slf4j.Slf4j;
import org.davidbohl.dirigent.deployments.events.DeploymentStateChangedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
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
    public void onDeploymentStateChanged(DeploymentStateChangedEvent event) {
        String title = "%s: \"%s\"".formatted(event.getState(), event.getDeploymentName());
        String context = event.getContext();
        sendGotifyMessage(title, context);

        log.info("Deployment '{}' state changed to {}. Context: {}", event.getDeploymentName(), event.getState(), context);
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
