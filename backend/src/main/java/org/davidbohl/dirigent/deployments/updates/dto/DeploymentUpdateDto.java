package org.davidbohl.dirigent.deployments.updates.dto;

public record DeploymentUpdateDto(String deploymentName, String service, String image, boolean isRunning) {

}
