package org.davidbohl.dirigent.deployments.updates.dto;

import java.util.List;

public record DeploymentUpdateDto(String deploymentName, List<DeploymentUpdateServiceImageDto> serviceUpdates) {

}
