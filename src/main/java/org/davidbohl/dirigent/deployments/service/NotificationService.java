package org.davidbohl.dirigent.deployments.service;

import org.davidbohl.dirigent.deployments.models.events.DeploymentStartFailedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
        if(gotifyToken != null && gotifyBaseUrl != null && !gotifyToken.isBlank() && !gotifyBaseUrl.isBlank()) {
            RestTemplate restTemplate = new RestTemplate();
            restTemplate.postForObject("%s/message?token=%s".formatted(gotifyBaseUrl, gotifyToken), new GotifyMessage("Deployment \"%s\" Failed".formatted(event.getDeploymentName()), event.getMessage(), 5), Object.class);
        }

        logger.warn("Deployment '{}' failed. Error: {}", event.getDeploymentName(), event.getMessage());

    }

    record GotifyMessage(String title, String message, int priority) {

    }

}
